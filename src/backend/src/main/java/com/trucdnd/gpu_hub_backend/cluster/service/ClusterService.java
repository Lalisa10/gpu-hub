package com.trucdnd.gpu_hub_backend.cluster.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.trucdnd.gpu_hub_backend.cluster.dto.*;
import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.mapper.ClusterMapper;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.common.constants.Cluster.Status;
import com.trucdnd.gpu_hub_backend.kubernetes.config.KubernetesProperties;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final ObjectStorageService objectStorageService;
    private final KubernetesProperties kubernetesProperties;
    private final BuiltinResourceService builtinResourceService;
    private final WorkloadRepository workloadRepository;

    public List<ClusterDto> findAll() {
        return clusterRepository.findAll()
                .stream()
                .map(ClusterMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public ClusterDto findById(UUID id) {
        return clusterRepository.findById(id)
                .map(ClusterMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + id));
    }

    public ClusterDto save(JoinClusterRequest joinClusterRequest) {
        Cluster cluster = ClusterMapper.toCluster(joinClusterRequest);
        cluster.setStatus(Status.ACTIVE);
        Cluster saved = clusterRepository.save(cluster);
        return ClusterMapper.toDto(saved);
    }

    public void delete(UUID id) {
        clusterRepository.deleteById(id);
    }

    public ClusterDto update(ClusterDto clusterDto) {
        Cluster cluster = clusterRepository.findById(clusterDto.id())
        .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterDto.id()));
        cluster.setName(clusterDto.name());
        cluster.setDescription(clusterDto.description());
        cluster.setStatus(clusterDto.status());
        Cluster saved = clusterRepository.save(cluster);
        return ClusterMapper.toDto(saved);
    }

    public ClusterDto update(UUID id, JoinClusterRequest request) {
        Cluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + id));
        cluster.setName(request.name());
        cluster.setDescription(request.description());
        cluster.setKubeconfigRef(request.kubeconfigRef());
        Cluster saved = clusterRepository.save(cluster);
        return ClusterMapper.toDto(saved);
    }

    public ClusterDto patch(UUID id, PatchClusterRequest request) {

        Cluster cluster = clusterRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + id));

        if (request.description().isPresent()) {
            cluster.setDescription(request.description().orElse(null));
        }

        if (request.name().isPresent()) {
            cluster.setName(request.name().orElse(null));
        }

        if (request.kubeconfigRef().isPresent()) {
            cluster.setKubeconfigRef(request.kubeconfigRef().orElse(null));
        }

        if (request.status().isPresent()) {
            cluster.setStatus(request.status().orElse(null));
        }

        return ClusterMapper.toDto(clusterRepository.save(cluster));
    }

    public ClusterDto uploadKubeconfig(UUID id, MultipartFile file) {
        Cluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + id));

        String objectKey = "clusters/" + id + "/kubeconfig";
        try {
            objectStorageService.putObject(
                    kubernetesProperties.getKubeconfigBucket(),
                    objectKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream"
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded kubeconfig file", e);
        }

        cluster.setKubeconfigRef(objectKey);
        return ClusterMapper.toDto(clusterRepository.save(cluster));
    }

    public ClusterDetailsDto getClusterDetails(UUID id) {
        Cluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found: " + id));

        List<Node> k8sNodes = builtinResourceService.listNodes(cluster);
        List<Pod> allPods = builtinResourceService.listAllPods(cluster);

        // Aggregate pod requests/limits by node (only schedulable pods)
        Map<String, NodeUsage> usageByNode = new HashMap<>();
        for (Pod pod : allPods) {
            String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : null;
            if (nodeName == null) continue;
            String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : null;
            if ("Succeeded".equals(phase) || "Failed".equals(phase)) continue;

            NodeUsage u = usageByNode.computeIfAbsent(nodeName, k -> new NodeUsage());
            if (pod.getSpec().getContainers() == null) continue;
            for (Container c : pod.getSpec().getContainers()) {
                ResourceRequirements rr = c.getResources();
                if (rr == null) continue;
                Map<String, Quantity> req = rr.getRequests();
                Map<String, Quantity> lim = rr.getLimits();
                if (req != null) {
                    u.cpuRequestMillis += parseCpuToMillis(amt(req.get("cpu")));
                    u.ramRequestBytes  += parseMemToBytes(amt(req.get("memory")));
                    u.gpuAllocated     += parseIntOr0(amt(req.get("nvidia.com/gpu")));
                }
                if (lim != null) {
                    u.cpuLimitMillis += parseCpuToMillis(amt(lim.get("cpu")));
                    u.ramLimitBytes  += parseMemToBytes(amt(lim.get("memory")));
                }
            }
        }

        List<NodeInfoDto> nodes = k8sNodes.stream().map(n -> {
            String status = computeNodeStatus(n);

            long cpuCap = parseCpuToMillis(n.getStatus().getCapacity()
                    .getOrDefault("cpu", new Quantity("0")).getAmount());
            long cpuAlloc = parseCpuToMillis(n.getStatus().getAllocatable()
                    .getOrDefault("cpu", new Quantity("0")).getAmount());
            long ramCap = parseMemToBytes(n.getStatus().getCapacity()
                    .getOrDefault("memory", new Quantity("0")).getAmount());
            long ramAlloc = parseMemToBytes(n.getStatus().getAllocatable()
                    .getOrDefault("memory", new Quantity("0")).getAmount());

            int gpuTotal = 0;
            try {
                gpuTotal = Integer.parseInt(n.getStatus().getCapacity()
                        .getOrDefault("nvidia.com/gpu", new Quantity("0")).getAmount());
            } catch (NumberFormatException ignored) {}

            String gpuModel = n.getMetadata().getLabels() != null
                    ? n.getMetadata().getLabels().get("nvidia.com/gpu.product") : null;

            NodeUsage u = usageByNode.getOrDefault(n.getMetadata().getName(), new NodeUsage());

            return new NodeInfoDto(
                    n.getMetadata().getName(),
                    status,
                    cpuCap, cpuAlloc, u.cpuRequestMillis, u.cpuLimitMillis,
                    ramCap, ramAlloc, u.ramRequestBytes, u.ramLimitBytes,
                    gpuTotal, Math.min(u.gpuAllocated, gpuTotal),
                    gpuModel);
        }).toList();

        int gpusTotal = nodes.stream().mapToInt(NodeInfoDto::gpuTotal).sum();
        int gpusInUse = nodes.stream().mapToInt(NodeInfoDto::gpuAllocated).sum();

        List<GpuInfoDto> gpus = new ArrayList<>();
        int globalIdx = 0;
        for (NodeInfoDto nDto : nodes) {
            for (int i = 0; i < nDto.gpuTotal(); i++) {
                gpus.add(new GpuInfoDto(globalIdx, nDto.name(), nDto.gpuModel(),
                        i < nDto.gpuAllocated() ? "In Use" : "Idle"));
                globalIdx++;
            }
        }

        List<Workload> running = workloadRepository
                .findByClusterIdAndStatus(id, com.trucdnd.gpu_hub_backend.common.constants.Workload.Status.RUNNING);
        List<ActiveWorkloadSummaryDto> activeWorkloads = running.stream()
                .map(w -> new ActiveWorkloadSummaryDto(w.getId(), w.getName(),
                        w.getWorkloadType().dbValue, w.getRequestedGpu(),
                        w.getStatus().dbValue, w.getStartedAt()))
                .toList();

        return new ClusterDetailsDto(cluster.getId(), cluster.getName(),
                gpusTotal, gpusInUse, nodes, gpus, activeWorkloads);
    }

    private static String computeNodeStatus(Node n) {
        if (n.getStatus() == null || n.getStatus().getConditions() == null) return "NotReady";
        boolean ready = false;
        boolean pressure = false;
        for (var c : n.getStatus().getConditions()) {
            String type = c.getType();
            boolean active = "True".equals(c.getStatus());
            if ("Ready".equals(type) && active) ready = true;
            if (active && (type.endsWith("Pressure") || "NetworkUnavailable".equals(type))) pressure = true;
        }
        if (!ready) return "NotReady";
        if (pressure) return "Pressure";
        return "Ready";
    }

    private static String amt(Quantity q) {
        return q == null ? null : q.getAmount() + (q.getFormat() == null ? "" : q.getFormat());
    }

    private static int parseIntOr0(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static final class NodeUsage {
        long cpuRequestMillis;
        long cpuLimitMillis;
        long ramRequestBytes;
        long ramLimitBytes;
        int gpuAllocated;
    }

    private long parseCpuToMillis(String q) {
        if (q == null || q.isBlank()) return 0;
        if (q.endsWith("m")) {
            try { return Long.parseLong(q.substring(0, q.length() - 1)); }
            catch (NumberFormatException e) { return 0; }
        }
        try { return (long) (Double.parseDouble(q) * 1000); }
        catch (NumberFormatException e) { return 0; }
    }

    private long parseMemToBytes(String q) {
        if (q == null || q.isBlank()) return 0;
        try {
            if (q.endsWith("Ki")) return Long.parseLong(q.replace("Ki", "")) * 1024L;
            if (q.endsWith("Mi")) return Long.parseLong(q.replace("Mi", "")) * 1024L * 1024;
            if (q.endsWith("Gi")) return Long.parseLong(q.replace("Gi", "")) * 1024L * 1024 * 1024;
            return Long.parseLong(q);
        } catch (NumberFormatException e) { return 0; }
    }

}
