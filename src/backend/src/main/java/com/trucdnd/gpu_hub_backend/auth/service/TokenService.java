package com.trucdnd.gpu_hub_backend.auth.service;

import com.trucdnd.gpu_hub_backend.config.JwtConfig;
import com.trucdnd.gpu_hub_backend.user.entity.RefreshToken;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.RefreshTokenRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public TokenService(RefreshTokenRepository refreshTokenRepository, JwtConfig jwtConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    public String generateRefreshToken(User user) {
        return generateRefreshToken(user, null);
    }

    public String generateRefreshToken(User user, RefreshToken parentToken) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .parent(parentToken)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpiration()))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public RefreshToken rotateAndValidate(String rawToken) throws AuthException {
        String hash = hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (Boolean.TRUE.equals(token.getRevoked())) {
            revokeTokenFamily(token);
            throw new AuthException("Refresh token reuse detected");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            token.setRevoked(true);
            token.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(token);
            throw new AuthException("Refresh token expired");
        }

        token.setRevoked(true);
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
        return token;
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    private void revokeTokenFamily(RefreshToken compromisedToken) {
        refreshTokenRepository.revokeAllByUserAndNotRevoked(
                compromisedToken.getUser(),
                OffsetDateTime.now()
        );
    }

    public String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
