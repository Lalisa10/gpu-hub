package com.trucdnd.gpu_hub_backend.team.repository;

import com.trucdnd.gpu_hub_backend.team.entity.TeamMember;
import com.trucdnd.gpu_hub_backend.team.entity.TeamMemberId;
import com.trucdnd.gpu_hub_backend.common.constants.Team.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
    boolean existsById_UserIdAndId_TeamId(UUID userId, UUID teamId);

    boolean existsById_UserIdAndId_TeamIdAndRole(UUID userId, UUID teamId, TeamRole role);
}
