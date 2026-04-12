package com.trucdnd.gpu_hub_backend.project.repository;

import com.trucdnd.gpu_hub_backend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    @Query("select p.team.id from Project p where p.id = :projectId")
    Optional<UUID> findTeamIdByProjectId(@Param("projectId") UUID projectId);

    @Query("""
            select count(p) > 0
            from Project p
            join TeamMember tm on tm.team.id = p.team.id
            where tm.id.userId = :userId
              and tm.role = com.trucdnd.gpu_hub_backend.common.constants.Team.TeamRole.TEAM_LEAD
              and p.cluster.id = :clusterId
            """)
    boolean existsLeadOwnedProjectInCluster(@Param("userId") UUID userId, @Param("clusterId") UUID clusterId);

    @Query("""
            select count(p) > 0
            from Project p
            join TeamMember tm on tm.team.id = p.team.id
            where tm.id.userId = :userId
              and tm.role = com.trucdnd.gpu_hub_backend.common.constants.Team.TeamRole.TEAM_LEAD
              and p.policy.id = :policyId
            """)
    boolean existsLeadOwnedProjectUsingPolicy(@Param("userId") UUID userId, @Param("policyId") UUID policyId);

    List<Project> findByPolicy_Id(UUID policyId);
}
