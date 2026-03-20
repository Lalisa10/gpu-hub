package com.trucdnd.gpu_hub_backend.team.entity;

import com.trucdnd.gpu_hub_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "team_members")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    @EmbeddedId
    private TeamMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;
}
