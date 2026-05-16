package com.trucdnd.gpu_hub_backend.workload_source.service;

import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import com.trucdnd.gpu_hub_backend.workload_source.dto.AttachSourceRequest;
import com.trucdnd.gpu_hub_backend.workload_source.dto.WorkloadSourceDto;
import com.trucdnd.gpu_hub_backend.workload_source.entity.WorkloadSource;
import com.trucdnd.gpu_hub_backend.workload_source.repository.WorkloadSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkloadSourceService {

    private final WorkloadSourceRepository workloadSourceRepository;
    private final WorkloadRepository workloadRepository;
    private final DataSourceRepository dataSourceRepository;

    public List<WorkloadSourceDto> findByWorkload(UUID workloadId) {
        return workloadSourceRepository.findByWorkload_Id(workloadId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public WorkloadSourceDto attach(UUID workloadId, AttachSourceRequest request) {
        Workload workload = workloadRepository.findById(workloadId)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found: " + workloadId));
        DataSource source = dataSourceRepository.findById(request.sourceId())
                .orElseThrow(() -> new EntityNotFoundException("DataSource not found: " + request.sourceId()));

        if (!source.getCluster().getId().equals(workload.getCluster().getId())) {
            throw new IllegalArgumentException("DataSource " + source.getId()
                    + " is on a different cluster than the workload");
        }
        if (!source.getTeam().getId().equals(workload.getProject().getTeam().getId())) {
            throw new IllegalArgumentException("DataSource " + source.getId()
                    + " belongs to a team that does not own the workload's project");
        }
        if (workloadSourceRepository.existsByWorkload_IdAndSource_Id(workloadId, request.sourceId())) {
            throw new IllegalArgumentException(
                    "Source " + request.sourceId() + " is already attached to workload " + workloadId);
        }

        WorkloadSource saved = workloadSourceRepository.save(WorkloadSource.builder()
                .workload(workload)
                .source(source)
                .mountPath(request.mountPath())
                .build());
        return toDto(saved);
    }

    @Transactional
    public void detach(UUID workloadId, UUID sourceId) {
        if (!workloadSourceRepository.existsByWorkload_IdAndSource_Id(workloadId, sourceId)) {
            throw new EntityNotFoundException("Source " + sourceId + " is not attached to workload " + workloadId);
        }
        workloadSourceRepository.deleteByWorkload_IdAndSource_Id(workloadId, sourceId);
    }

    private WorkloadSourceDto toDto(WorkloadSource ws) {
        return new WorkloadSourceDto(
                ws.getWorkload().getId(),
                ws.getSource().getId(),
                ws.getSource().getName(),
                ws.getMountPath()
        );
    }
}
