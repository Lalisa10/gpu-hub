package com.trucdnd.gpu_hub_backend.workload_volume.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkloadVolumeId implements Serializable {
    private UUID workload;
    private UUID volume;
}
