package com.eatmate.security.dto;


import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AccountContext implements UserDetails {

    private final AuthenticateAccountDto authenticateAccountDto;
    private final List<GrantedAuthority> authorities;

    @Builder
    public AccountContext(AuthenticateAccountDto authenticateAccountDto, List<GrantedAuthority> authorities) {
        this.authenticateAccountDto = authenticateAccountDto;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return authenticateAccountDto.getEmail();
    }

    @Override
    public String getPassword() {
        return authenticateAccountDto.getPassword();
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
