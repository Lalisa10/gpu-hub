package com.trucdnd.gpu_hub_backend.workload.entity;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workloads")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workload extends MutableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "workload_type", nullable = false)
    private com.trucdnd.gpu_hub_backend.common.constants.Workload.Type workloadType;

    @Column(name = "priority_class", nullable = false)
    private com.trucdnd.gpu_hub_backend.common.constants.Workload.PriorityClass priorityClass;

    @Column(name = "image", nullable = false)
    private String image;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "requested_gpu", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedGpu;

    @Column(name = "requested_cpu", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedCpu;

    @Column(name = "requested_memory", nullable = false)
    private Long requestedMemory;

    @Column(name = "status", nullable = false)
    private com.trucdnd.gpu_hub_backend.common.constants.Workload.Status status;

    @Column(name = "queued_at")
    private OffsetDateTime queuedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "extra", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String extra;
}
