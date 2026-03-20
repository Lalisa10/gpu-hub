package com.trucdnd.gpu_hub_backend.auth.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn
) {}
