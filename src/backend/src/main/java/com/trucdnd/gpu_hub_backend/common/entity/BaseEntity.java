package com.trucdnd.gpu_hub_backend.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter @Setter
public class BaseEntity implements Serializable {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.AUTO)
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}