package com.trucdnd.gpu_hub_backend.data_source.entity;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "data_sources",
        uniqueConstraints = @UniqueConstraint(name = "uq_data_sources_team_name", columnNames = {"team_id", "name"}))
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSource extends MutableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "folder_name", nullable = false)
    private String folderName;

    @Column(name = "status", nullable = false)
    private com.trucdnd.gpu_hub_backend.common.constants.DataSource.Status status;

    @Column(name = "bucket_url", nullable = false)
    private String bucketUrl;

    @Column(name = "access_key", nullable = false)
    private String accessKey;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;
}
