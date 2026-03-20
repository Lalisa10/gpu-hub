package com.trucdnd.gpu_hub_backend.auth.mapper;

import com.trucdnd.gpu_hub_backend.auth.dto.LoginRequest;
import com.trucdnd.gpu_hub_backend.auth.dto.RegisterRequest;
import com.trucdnd.gpu_hub_backend.auth.dto.UserResponse;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthMapper {
    private AuthMapper() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }
    public static User fromDto(final RegisterRequest createUserDto) {
        return User.builder()
                .email(createUserDto.email())
                .username(createUserDto.username())
                .build();
    }

    public static Authentication fromDto(final LoginRequest loginRequest) {
        return new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
    }

    public static UserResponse toDto(final User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getGlobalRole());
    }
}
