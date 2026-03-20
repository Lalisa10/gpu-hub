package com.trucdnd.gpu_hub_backend.auth.dto;

import com.trucdnd.gpu_hub_backend.common.constants.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank
    String username,
    @Email
    @NotBlank
    String email,
    @NotBlank
    String password,
    @NotBlank
    User globalRole
) {}
