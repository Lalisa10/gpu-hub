package com.trucdnd.gpu_hub_backend.user.service;

import com.trucdnd.gpu_hub_backend.user.dto.CreateUserRequest;
import com.trucdnd.gpu_hub_backend.user.dto.PatchUserRequest;
import com.trucdnd.gpu_hub_backend.user.dto.UpdateUserRequest;
import com.trucdnd.gpu_hub_backend.user.dto.UserDto;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    public UserDto findById(UUID id) {
        return toDto(getUser(id));
    }

    public UserDto create(CreateUserRequest request) {
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .globalRole(request.globalRole())
                .isActive(true)
                .build();
        return toDto(userRepository.save(user));
    }

    public UserDto update(UUID id, UpdateUserRequest request) {
        User user = getUser(id);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
        user.setGlobalRole(request.globalRole());
        user.setIsActive(request.isActive());
        return toDto(userRepository.save(user));
    }

    public UserDto patch(UUID id, PatchUserRequest request) {
        User user = getUser(id);

        if (request.username().isPresent()) {
            user.setUsername(request.username().orElse(null));
        }
        if (request.email().isPresent()) {
            user.setEmail(request.email().orElse(null));
        }
        if (request.fullName().isPresent()) {
            user.setFullName(request.fullName().orElse(null));
        }
        if (request.password().isPresent()) {
            user.setPasswordHash(passwordEncoder.encode(request.password().orElse(null)));
        }
        if (request.globalRole().isPresent()) {
            user.setGlobalRole(request.globalRole().orElse(null));
        }
        if (request.isActive().isPresent()) {
            user.setIsActive(request.isActive().orElse(null));
        }

        return toDto(userRepository.save(user));
    }

    public void delete(UUID id) {
        User user = getUser(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getGlobalRole(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
