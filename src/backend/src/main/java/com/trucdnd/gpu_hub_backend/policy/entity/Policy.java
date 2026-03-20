package com.trucdnd.gpu_hub_backend.policy.entity;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(
    name = "policies",
    uniqueConstraints = @UniqueConstraint(name = "uk_policies_cluster_name", columnNames = {"cluster_id", "name"})
)
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class Policy extends MutableEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "max_priority", nullable = false)
    private Integer maxPriority;

    @Column(name = "gpu_quota", precision = 10, scale = 2)
    private BigDecimal gpuQuota;

    @Column(name = "cpu_quota", precision = 10, scale = 2)
    private BigDecimal cpuQuota;

    @Column(name = "memory_quota")
    private Long memoryQuota;

    @Column(name = "gpu_limit", precision = 10, scale = 2)
    private BigDecimal gpuLimit;

    @Column(name = "cpu_limit", precision = 10, scale = 2)
    private BigDecimal cpuLimit;

    @Column(name = "memory_limit")
    private Long memoryLimit;

    @Column(name = "over_quota_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal overQuotaWeight;

    @Column(name = "node_affinity", columnDefinition = "jsonb")
    private String nodeAffinity;

    @Column(name = "gpu_types", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] gpuTypes;
}
