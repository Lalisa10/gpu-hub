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

    @ManyToOne(optional = false)
    @JoinColumn(name = "workload_type_id", nullable = false)
    private WorkloadType workloadType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "requested_gpu", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedGpu;

    @Column(name = "requested_cpu", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedCpu;

    @Column(name = "requested_memory", nullable = false)
    private Long requestedMemory;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "k8s_namespace")
    private String k8sNamespace;

    @Column(name = "k8s_resource_name")
    private String k8sResourceName;

    @Column(name = "k8s_resource_kind")
    private String k8sResourceKind;

    @Column(name = "queued_at")
    private OffsetDateTime queuedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "extra", columnDefinition = "jsonb")
    private String extra;
}
