package com.trucdnd.gpu_hub_backend.project.repository;

import com.trucdnd.gpu_hub_backend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}
