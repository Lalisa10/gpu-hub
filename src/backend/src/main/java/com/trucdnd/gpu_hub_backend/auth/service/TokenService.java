// package com.trucdnd.gpu_hub_backend.auth.service;

// import com.trucdnd.gpu_hub_backend.common.security.UserPrincipal;
// import com.trucdnd.gpu_hub_backend.config.JwtConfig;
// import com.trucdnd.gpu_hub_backend.user.entity.RefreshToken;
// import com.trucdnd.gpu_hub_backend.user.entity.User;
// import com.trucdnd.gpu_hub_backend.user.repository.RefreshTokenRepository;
// import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import jakarta.security.auth.message.AuthException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import javax.crypto.SecretKey;
// import java.nio.charset.StandardCharsets;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
// import java.time.Instant;
// import java.time.OffsetDateTime;
// import java.util.Base64;
// import java.util.Date;
// import java.util.UUID;

// @Service
// @RequiredArgsConstructor
// public class TokenService {

//     private final JwtConfig jwtConfig;
//     private final RefreshTokenRepository refreshTokenRepo;
//     private final SecretKey secretKey;
//     private final UserRepository userRepository;

//     public String generateAccessToken(UserPrincipal principal) {
//         return Jwts.builder()
//                 .subject(principal.getUserId().toString())
//                 .claim("username", principal.getUsername())
//                 .claim("globalRole", principal.getAuthorities())
//                 .claim("teamIds", principal.getTeamIds().stream()
//                         .map(UUID::toString)
//                         .toList())
//                 .issuedAt(new Date())
//                 .expiration(new Date(System.currentTimeMillis()
//                         + jwtConfig.getAccessTokenExpiration() * 1000))
//                 .signWith(secretKey)
//                 .compact();
//     }

//     public Claims parseAccessToken(String token) {
//         return Jwts.parser()
//                 .verifyWith(secretKey)
//                 .build()
//                 .parseSignedClaims(token)
//                 .getPayload();
//     }


//     public String generateRefreshToken(User user) {
//         return generateRefreshToken(user, null);
//     }

//     public String generateRefreshToken(User user, RefreshToken parentToken)  {
//         String rawToken = UUID.randomUUID().toString()
//                 + UUID.randomUUID().toString();

//         String tokenHash = hashToken(rawToken);


//         RefreshToken entity = RefreshToken.builder()
//                 .user(user)
//                 .tokenHash(tokenHash)
//                 .parent(parentToken)
//                 .expiresAt(OffsetDateTime.now().plusSeconds(
//                         jwtConfig.getRefreshTokenExpiration()))
//                 .build();

//         refreshTokenRepo.save(entity);
//         return rawToken;
//     }

//     @Transactional
//     public RefreshToken rotateAndValidate(String rawToken) throws AuthException {
//         String hash = hashToken(rawToken);

//         RefreshToken token = refreshTokenRepo.findByTokenHash(hash)
//                 .orElseThrow(() -> new AuthException("Invalid refresh token"));

//         if (token.getRevoked()) {
//             revokeTokenFamily(token);
//             throw new AuthException("Refresh token reuse detected");
//         }

//         if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
//             throw new AuthException("Refresh token expired");
//         }

//         token.setRevoked(true);
//         token.setRevokedAt(OffsetDateTime.now());
//         refreshTokenRepo.save(token);

//         return token;
//     }

//     private void revokeTokenFamily(RefreshToken compromisedToken) {
//         refreshTokenRepo.revokeAllByUserAndNotRevoked(
//                 compromisedToken.getUser(), OffsetDateTime.now()
//         );
//     }

//     public String hashToken(String raw) {
//         try {
//             MessageDigest digest = MessageDigest.getInstance("SHA-256");
//             byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
//             return Base64.getEncoder().encodeToString(hash);
//         } catch (NoSuchAlgorithmException e) {
//             throw new RuntimeException(e);
//         }
//     }
// }
