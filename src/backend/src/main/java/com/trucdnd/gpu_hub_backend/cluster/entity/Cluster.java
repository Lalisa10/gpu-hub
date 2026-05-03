package com.trucdnd.gpu_hub_backend.cluster.entity;

import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import com.trucdnd.gpu_hub_backend.common.constants.Cluster.Status;

@Entity
@Table(name = "clusters")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cluster extends MutableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "kubeconfig_ref")
    private String kubeconfigRef;

    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "juicefs_metaurl")
    private String juicefsMetaurl;

}
