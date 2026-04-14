package com.trucdnd.gpu_hub_backend.policy.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.QueueService;
import com.trucdnd.gpu_hub_backend.policy.dto.CreatePolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.dto.PatchPolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.dto.PolicyDto;
import com.trucdnd.gpu_hub_backend.policy.dto.UpdatePolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.policy.repository.PolicyRepository;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {
    private final PolicyRepository policyRepository;
    private final ClusterRepository clusterRepository;
    private final QueueService queueService;
    private final QueueSpecBuilder queueSpecBuilder;
    private final TeamClusterRepository teamClusterRepository;
    private final ProjectRepository projectRepository;

    public List<PolicyDto> findAll() {
        return policyRepository.findAll().stream().map(this::toDto).toList();
    }

    public PolicyDto findById(UUID id) {
        return toDto(getPolicy(id));
    }

    public PolicyDto create(CreatePolicyRequest request) {
        Policy policy = new Policy();
        apply(policy, request.clusterId(), request.name(), request.description(), request.priority(),
                request.gpuQuota(), request.cpuQuota(), request.memoryQuota(), request.gpuLimit(), request.cpuLimit(),
                request.memoryLimit(), request.gpuOverQuotaWeight(), request.cpuOverQuotaWeight(),
                request.memoryOverQuotaWeight(), request.nodeAffinity(), request.gpuTypes());
        return toDto(policyRepository.save(policy));
    }

    public PolicyDto update(UUID id, UpdatePolicyRequest request) {
        Policy policy = getPolicy(id);
        apply(policy, request.clusterId(), request.name(), request.description(), request.priority(),
                request.gpuQuota(), request.cpuQuota(), request.memoryQuota(), request.gpuLimit(), request.cpuLimit(),
                request.memoryLimit(), request.gpuOverQuotaWeight(), request.cpuOverQuotaWeight(),
                request.memoryOverQuotaWeight(), request.nodeAffinity(), request.gpuTypes());
        PolicyDto dto = toDto(policyRepository.save(policy));
        syncQueues(policy);
        return dto;
    }

    public PolicyDto patch(UUID id, PatchPolicyRequest request) {
        Policy policy = getPolicy(id);

        if (request.clusterId().isPresent()) {
            Cluster cluster = clusterRepository.findById(request.clusterId().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + request.clusterId().orElse(null)));
            policy.setCluster(cluster);
        }
        if (request.name().isPresent()) {
            policy.setName(request.name().orElse(null));
        }
        if (request.description().isPresent()) {
            policy.setDescription(request.description().orElse(null));
        }
        if (request.priority().isPresent()) {
            policy.setPriority(request.priority().orElse(null));
        }
        if (request.gpuQuota().isPresent()) {
            policy.setGpuQuota(request.gpuQuota().orElse(null));
        }
        if (request.cpuQuota().isPresent()) {
            policy.setCpuQuota(request.cpuQuota().orElse(null));
        }
        if (request.memoryQuota().isPresent()) {
            policy.setMemoryQuota(request.memoryQuota().orElse(null));
        }
        if (request.gpuLimit().isPresent()) {
            policy.setGpuLimit(request.gpuLimit().orElse(null));
        }
        if (request.cpuLimit().isPresent()) {
            policy.setCpuLimit(request.cpuLimit().orElse(null));
        }
        if (request.memoryLimit().isPresent()) {
            policy.setMemoryLimit(request.memoryLimit().orElse(null));
        }
        if (request.gpuOverQuotaWeight().isPresent()) {
            policy.setGpuOverQuotaWeight(request.gpuOverQuotaWeight().orElse(null));
        }
        if (request.cpuOverQuotaWeight().isPresent()) {
            policy.setCpuOverQuotaWeight(request.cpuOverQuotaWeight().orElse(null));
        }
        if (request.memoryOverQuotaWeight().isPresent()) {
            policy.setMemoryOverQuotaWeight(request.memoryOverQuotaWeight().orElse(null));
        }
        if (request.nodeAffinity().isPresent()) {
            policy.setNodeAffinity(request.nodeAffinity().orElse(null));
        }
        if (request.gpuTypes().isPresent()) {
            policy.setGpuTypes(request.gpuTypes().orElse(null));
        }

        PolicyDto dto = toDto(policyRepository.save(policy));
        syncQueues(policy);
        return dto;
    }

    public void delete(UUID id) {
        Policy policy = getPolicy(id);
        deleteQueues(policy);
        policyRepository.delete(policy);
    }

    private void apply(
            Policy policy,
            UUID clusterId,
            String name,
            String description,
            Integer priority,
            java.math.BigDecimal gpuQuota,
            java.math.BigDecimal cpuQuota,
            Long memoryQuota,
            java.math.BigDecimal gpuLimit,
            java.math.BigDecimal cpuLimit,
            Long memoryLimit,
            Integer gpuOverQuotaWeight,
            Integer cpuOverQuotaWeight,
            Integer memoryOverQuotaWeight,
            Map<String, Object> nodeAffinity,
            String[] gpuTypes) {
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));

        policy.setCluster(cluster);
        policy.setName(name);
        policy.setDescription(description);
        policy.setPriority(priority);
        policy.setGpuQuota(gpuQuota);
        policy.setCpuQuota(cpuQuota);
        policy.setMemoryQuota(memoryQuota);
        policy.setGpuLimit(gpuLimit);
        policy.setCpuLimit(cpuLimit);
        policy.setMemoryLimit(memoryLimit);
        policy.setGpuOverQuotaWeight(gpuOverQuotaWeight);
        policy.setCpuOverQuotaWeight(cpuOverQuotaWeight);
        policy.setMemoryOverQuotaWeight(memoryOverQuotaWeight);
        policy.setNodeAffinity(nodeAffinity);
        policy.setGpuTypes(gpuTypes);
    }

    private void syncQueues(Policy policy) {
        for (TeamCluster tc : teamClusterRepository.findByPolicy_Id(policy.getId())) {
            GenericKubernetesResource queue = queueSpecBuilder.buildTeamQueue(tc);
            queueService.update(tc.getCluster(), queue);
        }
        for (Project project : projectRepository.findByPolicy_Id(policy.getId())) {
            TeamCluster tc = teamClusterRepository
                    .findByTeam_IdAndCluster_Id(project.getTeam().getId(), project.getCluster().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "TeamCluster not found for team=" + project.getTeam().getId()
                                    + " cluster=" + project.getCluster().getId()));
            String parent = queueSpecBuilder.buildTeamQueueName(tc);
            GenericKubernetesResource queue = queueSpecBuilder.buildProjectQueue(project, parent);
            queueService.update(project.getCluster(), queue);
        }
    }

    private void deleteQueues(Policy policy) {
        for (TeamCluster tc : teamClusterRepository.findByPolicy_Id(policy.getId())) {
            queueService.delete(tc.getCluster(), null, queueSpecBuilder.buildTeamQueueName(tc));
        }
        for (Project project : projectRepository.findByPolicy_Id(policy.getId())) {
            queueService.delete(project.getCluster(), null, queueSpecBuilder.buildProjectQueueName(project));
        }
    }

    private Policy getPolicy(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + id));
    }

    private PolicyDto toDto(Policy policy) {
        return new PolicyDto(
                policy.getId(),
                policy.getCluster().getId(),
                policy.getName(),
                policy.getDescription(),
                policy.getPriority(),
                policy.getGpuQuota(),
                policy.getCpuQuota(),
                policy.getMemoryQuota(),
                policy.getGpuLimit(),
                policy.getCpuLimit(),
                policy.getMemoryLimit(),
                policy.getGpuOverQuotaWeight(),
                policy.getCpuOverQuotaWeight(),
                policy.getMemoryOverQuotaWeight(),
                policy.getNodeAffinity(),
                policy.getGpuTypes(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
