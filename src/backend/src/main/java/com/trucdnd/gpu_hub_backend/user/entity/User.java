package com.trucdnd.gpu_hub_backend.user.entity;

import com.trucdnd.gpu_hub_backend.common.entity.MutableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends MutableEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "Email should be valid")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "global_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private GlobalRole globalRole;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Transient
    private Set<UUID> teamIds;
    
}
