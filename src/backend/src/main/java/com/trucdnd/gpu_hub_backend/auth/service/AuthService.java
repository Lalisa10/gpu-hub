// package com.trucdnd.gpu_hub_backend.auth.service;

// import com.trucdnd.gpu_hub_backend.auth.dto.LoginRequest;
// import com.trucdnd.gpu_hub_backend.auth.dto.TokenResponse;
// import com.trucdnd.gpu_hub_backend.common.security.UserPrincipal;
// import com.trucdnd.gpu_hub_backend.config.JwtConfig;
// import com.trucdnd.gpu_hub_backend.user.entity.RefreshToken;
// import com.trucdnd.gpu_hub_backend.user.entity.User;
// import com.trucdnd.gpu_hub_backend.user.repository.RefreshTokenRepository;
// import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
// import jakarta.security.auth.message.AuthException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;

// import java.time.OffsetDateTime;

// @Service
// @RequiredArgsConstructor
// public class AuthService {

//     private final UserRepository userRepo;
//     private final TokenService tokenService;
//     private final PasswordEncoder passwordEncoder;
//     private final RefreshTokenRepository refreshTokenRepo;
//     private final JwtConfig jwtConfig;

//     public TokenResponse login(LoginRequest req) throws AuthException {
//         User user = userRepo.findByUsername(req.username())
//                 .filter(User::getIsActive)
//                 .orElseThrow(() -> new AuthException("Invalid credentials"));

//         if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
//             throw new AuthException("Invalid credentials");
//         }

//         return issueTokenPair(user);
//     }

//     public TokenResponse refresh(String rawRefreshToken) throws AuthException {
//         RefreshToken old = tokenService.rotateAndValidate(rawRefreshToken);

//         User user = old.getUser();

//         return issueTokenPair(user, old);
//     }

//     public void logout(String rawRefreshToken) {
//         String hash = tokenService.hashToken(rawRefreshToken);
//         refreshTokenRepo.findByTokenHash(hash)
//                 .ifPresent(t -> {
//                     t.setRevoked(true);
//                     t.setRevokedAt(OffsetDateTime.now());
//                     refreshTokenRepo.save(t);
//                 });
//     }

//     private TokenResponse issueTokenPair(User user) {
//         return issueTokenPair(user, null);
//     }

//     private TokenResponse issueTokenPair(User user, RefreshToken parentToken) {
//         UserPrincipal principal = new UserPrincipal(user);

//         String accessToken  = tokenService.generateAccessToken(principal);
//         String refreshToken = tokenService.generateRefreshToken(
//                 user, parentToken);

//         return TokenResponse.builder()
//                 .accessToken(accessToken)
//                 .refreshToken(refreshToken)
//                 .expiresIn(jwtConfig.getAccessTokenExpiration())
//                 .build();
//     }
// }