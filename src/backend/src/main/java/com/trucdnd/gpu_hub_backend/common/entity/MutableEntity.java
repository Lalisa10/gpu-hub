package com.trucdnd.gpu_hub_backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter @Setter
public class MutableEntity extends BaseEntity {
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
