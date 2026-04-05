package com.trucdnd.gpu_hub_backend.auth.service;

import com.trucdnd.gpu_hub_backend.auth.dto.LoginRequest;
import com.trucdnd.gpu_hub_backend.auth.dto.TokenResponse;
import com.trucdnd.gpu_hub_backend.config.JwtConfig;
import com.trucdnd.gpu_hub_backend.user.entity.RefreshToken;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final JwtConfig jwtConfig;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenService tokenService,
            JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.jwtConfig = jwtConfig;
    }

    public TokenResponse login(LoginRequest request) throws AuthException {
        User user = userRepository.findByUsername(request.username())
                .filter(User::getIsActive)
                .orElseThrow(() -> new AuthException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid username or password");
        }

        return issueTokenPair(user, null);
    }

    public TokenResponse refresh(String rawRefreshToken) throws AuthException {
        RefreshToken oldToken = tokenService.rotateAndValidate(rawRefreshToken);
        return issueTokenPair(oldToken.getUser(), oldToken);
    }

    public void logout(String rawRefreshToken) {
        tokenService.revoke(rawRefreshToken);
    }

    private TokenResponse issueTokenPair(User user, RefreshToken parentToken) {
        return TokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(tokenService.generateRefreshToken(user, parentToken))
                .expiresIn(jwtConfig.getAccessTokenExpiration())
                .build();
    }
}
