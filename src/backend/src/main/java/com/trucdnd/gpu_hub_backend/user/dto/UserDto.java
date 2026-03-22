package com.trucdnd.gpu_hub_backend.user.dto;

import com.trucdnd.gpu_hub_backend.common.constants.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String fullName,
        User.GlobalRole globalRole,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
