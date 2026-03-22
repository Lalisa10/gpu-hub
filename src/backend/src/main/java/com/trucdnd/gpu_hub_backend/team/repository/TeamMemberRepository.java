package com.trucdnd.gpu_hub_backend.team.repository;

import com.trucdnd.gpu_hub_backend.team.entity.TeamMember;
import com.trucdnd.gpu_hub_backend.team.entity.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
}
