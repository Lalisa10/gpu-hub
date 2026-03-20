package com.trucdnd.gpu_hub_backend.user.entity;

import com.trucdnd.gpu_hub_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

import java.time.OffsetDateTime;

@Table(name = "refresh_tokens")
@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    @Default
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private RefreshToken parent;
}
