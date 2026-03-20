package com.trucdnd.gpu_hub_backend.team.entity;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
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
@Table(
    name = "team_clusters",
    uniqueConstraints = @UniqueConstraint(name = "uk_team_clusters_team_cluster", columnNames = {"team_id", "cluster_id"})
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCluster extends MutableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "namespace", nullable = false)
    private String namespace;
}
