package com.trucdnd.gpu_hub_backend.auth.service;

import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import com.trucdnd.gpu_hub_backend.common.security.UserPrincipal;
import com.trucdnd.gpu_hub_backend.config.JwtConfig;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final JwtConfig jwtConfig;

    public JwtService(SecretKey secretKey, JwtConfig jwtConfig) {
        this.secretKey = secretKey;
        this.jwtConfig = jwtConfig;
    }

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        long expirationMillis = now + (jwtConfig.getAccessTokenExpiration() * 1000L);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getGlobalRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    public UserPrincipal parsePrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        return new UserPrincipal(userId, username, GlobalRole.valueOf(role));
    }
}
