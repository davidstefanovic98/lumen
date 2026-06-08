package io.lumen.security.authentication;

import io.lumen.security.authority.GrantedAuthority;
import io.lumen.security.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UsernamePasswordAuthenticationToken implements Authentication {
    private final Object principal;
    private final Object credentials;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated;

    public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = new ArrayList<>();
        this.authenticated = false;
    }

    public UsernamePasswordAuthenticationToken(Object principal,
                                               Object credentials,
                                               Collection<? extends GrantedAuthority> authorities) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = (authorities == null) ? new ArrayList<>() : authorities;
        this.authenticated = true; // This constructor is the "Seal of Approval"
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return principal != null ? principal.toString() : "";
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
