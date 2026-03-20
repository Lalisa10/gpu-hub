package com.trucdnd.gpu_hub_backend.user.repository;

import com.trucdnd.gpu_hub_backend.user.entity.RefreshToken;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    List<RefreshToken> findByUserId(UUID userId);

    List<RefreshToken> findByParentId(UUID parentId);

    Optional<RefreshToken>  findByTokenHash(String hash);

    @Modifying
    @Transactional
    @Query("""
        UPDATE RefreshToken t
        SET t.revoked = true, t.revokedAt = :now
        WHERE t.user = :user
          AND t.revoked = false
    """)
    int revokeAllByUserAndNotRevoked(
            @Param("user") User user,
            @Param("now")  OffsetDateTime now
    );
}