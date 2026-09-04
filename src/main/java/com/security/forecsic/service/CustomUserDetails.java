package com.security.forecsic.service;

import com.security.forecsic.model.Register;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Register user;

    public CustomUserDetails(Register user) {
        this.user = user;
    }

    public Register getUser() {
        return user;
    }

    public Integer getId() {
        return user != null ? user.getId() : null;
    }

    public String getFullName() {
        return user != null ? user.getFullName() : null;
    }

    public String getRole() {
        return user != null ? user.getRole() : "USER";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = (user != null && user.getRole() != null) ? user.getRole().toUpperCase() : "USER";
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getEmail() : null;
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
        return user != null && user.isVerified();
    }
}
