package com.trucdnd.gpu_hub_backend.auth.dto;

import com.trucdnd.gpu_hub_backend.common.constants.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        User.GlobalRole globalRole
) {}