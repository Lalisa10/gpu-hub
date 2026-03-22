package com.trucdnd.gpu_hub_backend.policy.controller;

import com.trucdnd.gpu_hub_backend.policy.dto.CreatePolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.dto.PatchPolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.dto.PolicyDto;
import com.trucdnd.gpu_hub_backend.policy.dto.UpdatePolicyRequest;
import com.trucdnd.gpu_hub_backend.policy.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {
    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<List<PolicyDto>> getAll() {
        return ResponseEntity.ok(policyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PolicyDto> create(@RequestBody @Valid CreatePolicyRequest request) {
        PolicyDto saved = policyService.create(request);
        return ResponseEntity.created(URI.create("/api/policies/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyDto> update(@PathVariable UUID id, @RequestBody @Valid UpdatePolicyRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PolicyDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchPolicyRequest request) {
        return ResponseEntity.ok(policyService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
