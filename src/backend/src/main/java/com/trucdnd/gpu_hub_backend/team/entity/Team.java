package com.trucdnd.gpu_hub_backend.team.entity;

import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team extends MutableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;
}
