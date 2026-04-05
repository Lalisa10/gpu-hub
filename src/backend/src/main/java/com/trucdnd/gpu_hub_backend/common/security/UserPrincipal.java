package com.trucdnd.gpu_hub_backend.common.security;

import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal {

    private final UUID userId;
    private final String username;
    private final GlobalRole globalRole;

    public UserPrincipal(UUID userId, String username, GlobalRole globalRole) {
        this.userId = userId;
        this.username = username;
        this.globalRole = globalRole;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public GlobalRole getGlobalRole() {
        return globalRole;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + globalRole.name()));
    }
}
