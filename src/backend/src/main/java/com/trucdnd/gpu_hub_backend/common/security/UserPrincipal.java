package com.trucdnd.gpu_hub_backend.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.trucdnd.gpu_hub_backend.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter @Setter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    private final User user;
    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of (
                new SimpleGrantedAuthority("ROLE_" + user.getGlobalRole())
        );
    }

    public UUID getUserId() { return user.getId(); }

    public Set<UUID> getTeamIds() { return user.getTeamIds(); }
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    @NullMarked
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsActive();
    }
    
}
