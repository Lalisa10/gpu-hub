package com.trucdnd.gpu_hub_backend.user.dto;

import com.trucdnd.gpu_hub_backend.common.constants.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        String fullName,
        @NotBlank String password,
        @NotNull User.GlobalRole globalRole
) {
}
