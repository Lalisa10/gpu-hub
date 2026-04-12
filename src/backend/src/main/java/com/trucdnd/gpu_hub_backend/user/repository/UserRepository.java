package com.trucdnd.gpu_hub_backend.user.repository;

import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByGlobalRole(GlobalRole globalRole);
}
