package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadTypeRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PatchWorkloadTypeRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.UpdateWorkloadTypeRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadTypeDto;
import com.trucdnd.gpu_hub_backend.workload.entity.WorkloadType;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkloadTypeService {
    private final WorkloadTypeRepository workloadTypeRepository;

    public List<WorkloadTypeDto> findAll() {
        return workloadTypeRepository.findAll().stream().map(this::toDto).toList();
    }

    public WorkloadTypeDto findById(UUID id) {
        return toDto(getWorkloadType(id));
    }

    public WorkloadTypeDto create(CreateWorkloadTypeRequest request) {
        WorkloadType entity = new WorkloadType();
        apply(entity, request.name(), request.displayName(), request.description(), request.defaultGpu(), request.defaultCpu(),
                request.defaultMemory(), request.priorityClass(), request.isActive());
        return toDto(workloadTypeRepository.save(entity));
    }

    public WorkloadTypeDto update(UUID id, UpdateWorkloadTypeRequest request) {
        WorkloadType entity = getWorkloadType(id);
        apply(entity, request.name(), request.displayName(), request.description(), request.defaultGpu(), request.defaultCpu(),
                request.defaultMemory(), request.priorityClass(), request.isActive());
        return toDto(workloadTypeRepository.save(entity));
    }

    public WorkloadTypeDto patch(UUID id, PatchWorkloadTypeRequest request) {
        WorkloadType entity = getWorkloadType(id);

        if (request.name().isPresent()) {
            entity.setName(request.name().orElse(null));
        }
        if (request.displayName().isPresent()) {
            entity.setDisplayName(request.displayName().orElse(null));
        }
        if (request.description().isPresent()) {
            entity.setDescription(request.description().orElse(null));
        }
        if (request.defaultGpu().isPresent()) {
            entity.setDefaultGpu(request.defaultGpu().orElse(null));
        }
        if (request.defaultCpu().isPresent()) {
            entity.setDefaultCpu(request.defaultCpu().orElse(null));
        }
        if (request.defaultMemory().isPresent()) {
            entity.setDefaultMemory(request.defaultMemory().orElse(null));
        }
        if (request.priorityClass().isPresent()) {
            entity.setPriorityClass(request.priorityClass().orElse(null));
        }
        if (request.isActive().isPresent()) {
            entity.setIsActive(request.isActive().orElse(null));
        }

        return toDto(workloadTypeRepository.save(entity));
    }

    public void delete(UUID id) {
        workloadTypeRepository.delete(getWorkloadType(id));
    }

    private void apply(WorkloadType entity, String name, String displayName, String description,
                       java.math.BigDecimal defaultGpu, java.math.BigDecimal defaultCpu, Long defaultMemory,
                       String priorityClass, Boolean isActive) {
        entity.setName(name);
        entity.setDisplayName(displayName);
        entity.setDescription(description);
        entity.setDefaultGpu(defaultGpu);
        entity.setDefaultCpu(defaultCpu);
        entity.setDefaultMemory(defaultMemory);
        entity.setPriorityClass(priorityClass);
        entity.setIsActive(isActive);
    }

    private WorkloadType getWorkloadType(UUID id) {
        return workloadTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkloadType not found with id: " + id));
    }

    private WorkloadTypeDto toDto(WorkloadType entity) {
        return new WorkloadTypeDto(
                entity.getId(),
                entity.getName(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getDefaultGpu(),
                entity.getDefaultCpu(),
                entity.getDefaultMemory(),
                entity.getPriorityClass(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
