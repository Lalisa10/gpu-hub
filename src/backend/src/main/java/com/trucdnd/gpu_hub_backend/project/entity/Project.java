package com.trucdnd.gpu_hub_backend.project.entity;

import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "projects",
    uniqueConstraints = @UniqueConstraint(name = "uk_projects_team_name", columnNames = {"team_id", "name"})
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project extends MutableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "mlflow_experiment_id")
    private String mlflowExperimentId;

    @Column(name = "minio_prefix")
    private String minioPrefix;
}
