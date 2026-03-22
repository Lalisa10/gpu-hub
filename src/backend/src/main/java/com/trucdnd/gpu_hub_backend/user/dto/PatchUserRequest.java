package com.trucdnd.gpu_hub_backend.user.dto;

import com.trucdnd.gpu_hub_backend.common.constants.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Optional;

@Builder
public record PatchUserRequest(
        Optional<@NotBlank String> username,
        Optional<@NotBlank @Email String> email,
        Optional<String> fullName,
        Optional<@NotBlank String> password,
        Optional<User.GlobalRole> globalRole,
        Optional<Boolean> isActive
) {
}
