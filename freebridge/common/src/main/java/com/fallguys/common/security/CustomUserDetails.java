package com.fallguys.common.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String name;
    private final String role;
    private final String grade;
    private final Collection<? extends GrantedAuthority> authorities;

    @Builder
    public CustomUserDetails(Long id, String email, String name, String role, String grade,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.grade = grade;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // JWT 인증에서는 비밀번호를 사용하지 않음
    }

    @Override
    public String getUsername() {
        return email;
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
        return true;
    }
}
