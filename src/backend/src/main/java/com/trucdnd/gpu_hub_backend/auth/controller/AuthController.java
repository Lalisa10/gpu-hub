package com.trucdnd.gpu_hub_backend.auth.controller;

import com.trucdnd.gpu_hub_backend.auth.dto.LoginRequest;
import com.trucdnd.gpu_hub_backend.auth.dto.RefreshTokenRequest;
import com.trucdnd.gpu_hub_backend.auth.dto.TokenResponse;
import com.trucdnd.gpu_hub_backend.auth.service.AuthService;
import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) throws AuthException {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) throws AuthException {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
