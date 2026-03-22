package com.trucdnd.gpu_hub_backend.project.service;

import com.trucdnd.gpu_hub_backend.project.dto.CreateProjectRequest;
import com.trucdnd.gpu_hub_backend.project.dto.PatchProjectRequest;
import com.trucdnd.gpu_hub_backend.project.dto.ProjectDto;
import com.trucdnd.gpu_hub_backend.project.dto.UpdateProjectRequest;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;

    public List<ProjectDto> findAll() {
        return projectRepository.findAll().stream().map(this::toDto).toList();
    }

    public ProjectDto findById(UUID id) {
        return toDto(getProject(id));
    }

    public ProjectDto create(CreateProjectRequest request) {
        Project project = new Project();
        apply(project, request.teamId(), request.name(), request.description(), request.mlflowExperimentId(), request.minioPrefix());
        return toDto(projectRepository.save(project));
    }

    public ProjectDto update(UUID id, UpdateProjectRequest request) {
        Project project = getProject(id);
        apply(project, request.teamId(), request.name(), request.description(), request.mlflowExperimentId(), request.minioPrefix());
        return toDto(projectRepository.save(project));
    }

    public ProjectDto patch(UUID id, PatchProjectRequest request) {
        Project project = getProject(id);

        if (request.teamId().isPresent()) {
            Team team = teamRepository.findById(request.teamId().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.teamId().orElse(null)));
            project.setTeam(team);
        }
        if (request.name().isPresent()) {
            project.setName(request.name().orElse(null));
        }
        if (request.description().isPresent()) {
            project.setDescription(request.description().orElse(null));
        }
        if (request.mlflowExperimentId().isPresent()) {
            project.setMlflowExperimentId(request.mlflowExperimentId().orElse(null));
        }
        if (request.minioPrefix().isPresent()) {
            project.setMinioPrefix(request.minioPrefix().orElse(null));
        }

        return toDto(projectRepository.save(project));
    }

    public void delete(UUID id) {
        projectRepository.delete(getProject(id));
    }

    private void apply(Project project, UUID teamId, String name, String description, String mlflowExperimentId, String minioPrefix) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));

        project.setTeam(team);
        project.setName(name);
        project.setDescription(description);
        project.setMlflowExperimentId(mlflowExperimentId);
        project.setMinioPrefix(minioPrefix);
    }

    private Project getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getTeam().getId(),
                project.getName(),
                project.getDescription(),
                project.getMlflowExperimentId(),
                project.getMinioPrefix(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
