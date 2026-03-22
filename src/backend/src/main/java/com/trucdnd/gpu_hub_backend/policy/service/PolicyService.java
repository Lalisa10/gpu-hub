package com.trucdnd.gpu_hub_backend.policy.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.policy.dto.CreatePolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.dto.PatchPolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.dto.PolicyDto;
import com.trucdnd.gpu_hub_backend.policy.dto.UpdatePolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.policy.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {
    private final PolicyRepository policyRepository;
    private final ClusterRepository clusterRepository;

    public List<PolicyDto> findAll() {
        return policyRepository.findAll().stream().map(this::toDto).toList();
    }

    public PolicyDto findById(UUID id) {
        return toDto(getPolicy(id));
    }

    public PolicyDto create(CreatePolicyRequest request) {
        Policy policy = new Policy();
        apply(policy, request.clusterId(), request.name(), request.description(), request.maxPriority(),
                request.gpuQuota(), request.cpuQuota(), request.memoryQuota(), request.gpuLimit(), request.cpuLimit(),
                request.memoryLimit(), request.overQuotaWeight(), request.nodeAffinity(), request.gpuTypes());
        return toDto(policyRepository.save(policy));
    }

    public PolicyDto update(UUID id, UpdatePolicyRequest request) {
        Policy policy = getPolicy(id);
        apply(policy, request.clusterId(), request.name(), request.description(), request.maxPriority(),
                request.gpuQuota(), request.cpuQuota(), request.memoryQuota(), request.gpuLimit(), request.cpuLimit(),
                request.memoryLimit(), request.overQuotaWeight(), request.nodeAffinity(), request.gpuTypes());
        return toDto(policyRepository.save(policy));
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
        if (request.maxPriority().isPresent()) {
            policy.setMaxPriority(request.maxPriority().orElse(null));
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
        if (request.overQuotaWeight().isPresent()) {
            policy.setOverQuotaWeight(request.overQuotaWeight().orElse(null));
        }
        if (request.nodeAffinity().isPresent()) {
            policy.setNodeAffinity(request.nodeAffinity().orElse(null));
        }
        if (request.gpuTypes().isPresent()) {
            policy.setGpuTypes(request.gpuTypes().orElse(null));
        }

        return toDto(policyRepository.save(policy));
    }

    public void delete(UUID id) {
        policyRepository.delete(getPolicy(id));
    }

    private void apply(
            Policy policy,
            UUID clusterId,
            String name,
            String description,
            Integer maxPriority,
            java.math.BigDecimal gpuQuota,
            java.math.BigDecimal cpuQuota,
            Long memoryQuota,
            java.math.BigDecimal gpuLimit,
            java.math.BigDecimal cpuLimit,
            Long memoryLimit,
            java.math.BigDecimal overQuotaWeight,
            String nodeAffinity,
            String[] gpuTypes) {
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));

        policy.setCluster(cluster);
        policy.setName(name);
        policy.setDescription(description);
        policy.setMaxPriority(maxPriority);
        policy.setGpuQuota(gpuQuota);
        policy.setCpuQuota(cpuQuota);
        policy.setMemoryQuota(memoryQuota);
        policy.setGpuLimit(gpuLimit);
        policy.setCpuLimit(cpuLimit);
        policy.setMemoryLimit(memoryLimit);
        policy.setOverQuotaWeight(overQuotaWeight);
        policy.setNodeAffinity(nodeAffinity);
        policy.setGpuTypes(gpuTypes);
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
                policy.getMaxPriority(),
                policy.getGpuQuota(),
                policy.getCpuQuota(),
                policy.getMemoryQuota(),
                policy.getGpuLimit(),
                policy.getCpuLimit(),
                policy.getMemoryLimit(),
                policy.getOverQuotaWeight(),
                policy.getNodeAffinity(),
                policy.getGpuTypes(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
