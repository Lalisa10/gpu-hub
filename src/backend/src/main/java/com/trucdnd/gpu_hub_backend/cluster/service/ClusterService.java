package com.trucdnd.gpu_hub_backend.cluster.service;

import java.io.IOException;
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
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final ObjectStorageService objectStorageService;
    private final KubernetesProperties kubernetesProperties;

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

}
