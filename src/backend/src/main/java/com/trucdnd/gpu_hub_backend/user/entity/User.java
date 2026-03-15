package com.trucdnd.gpu_hub_backend.user.entity;

import com.trucdnd.gpu_hub_backend.common.contants.GlobalRoles;
import com.trucdnd.gpu_hub_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "Email should be valid")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "global_role")
    @Enumerated(EnumType.STRING)
    private GlobalRoles globalRole;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private Set<UUID> teamIds;
}
