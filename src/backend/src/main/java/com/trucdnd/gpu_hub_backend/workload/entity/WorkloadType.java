package com.trucdnd.gpu_hub_backend.workload.entity;

import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "workload_types")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadType extends MutableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "default_gpu", precision = 10, scale = 2)
    private BigDecimal defaultGpu;

    @Column(name = "default_cpu", precision = 10, scale = 2)
    private BigDecimal defaultCpu;

    @Column(name = "default_memory")
    private Long defaultMemory;

    @Column(name = "priority_class", nullable = false)
    private String priorityClass;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
