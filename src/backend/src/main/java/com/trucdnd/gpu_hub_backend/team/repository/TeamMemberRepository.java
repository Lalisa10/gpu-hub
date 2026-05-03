package com.trucdnd.gpu_hub_backend.team.repository;

import com.trucdnd.gpu_hub_backend.team.entity.TeamMember;
import com.trucdnd.gpu_hub_backend.team.entity.TeamMemberId;
import com.trucdnd.gpu_hub_backend.common.constants.Team.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
    boolean existsById_UserIdAndId_TeamId(UUID userId, UUID teamId);

    boolean existsById_UserIdAndId_TeamIdAndRole(UUID userId, UUID teamId, TeamRole role);

    @Query("select tm.id.teamId from TeamMember tm where tm.id.userId = :userId")
    List<UUID> findTeamIdsByUserId(@Param("userId") UUID userId);
}
