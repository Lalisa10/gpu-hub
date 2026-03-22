package com.trucdnd.gpu_hub_backend.policy.repository;

import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
}
