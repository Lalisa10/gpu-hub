package com.trucdnd.gpu_hub_backend.user.controller;

import com.trucdnd.gpu_hub_backend.user.dto.CreateUserRequest;
import com.trucdnd.gpu_hub_backend.user.dto.PatchUserRequest;
import com.trucdnd.gpu_hub_backend.user.dto.UpdateUserRequest;
import com.trucdnd.gpu_hub_backend.user.dto.UserDto;
import com.trucdnd.gpu_hub_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody @Valid CreateUserRequest request) {
        UserDto saved = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchUserRequest request) {
        return ResponseEntity.ok(userService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
