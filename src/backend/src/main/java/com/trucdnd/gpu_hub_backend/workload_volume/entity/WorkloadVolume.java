package com.trucdnd.gpu_hub_backend.workload_volume.entity;

import com.trucdnd.gpu_hub_backend.data_volume.entity.DataVolume;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workload_volumes")
@IdClass(WorkloadVolumeId.class)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadVolume {

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "workload_id", nullable = false)
    private Workload workload;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "volume_id", nullable = false)
    private DataVolume volume;

    @Column(name = "mount_path", nullable = false)
    private String mountPath;
}
