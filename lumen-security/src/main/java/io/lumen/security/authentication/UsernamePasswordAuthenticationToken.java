package io.lumen.security.authentication;

import java.util.Collection;

public class UsernamePasswordAuthenticationToken implements Authentication {
    private final Object principal;
    private final Object credentials;
    private final Collection<String> authorities;
    private boolean authenticated;

    public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = null;
        this.authenticated = false;
    }

    public UsernamePasswordAuthenticationToken(Object principal, Collection<String> authorities) {
        this.principal = principal;
        this.credentials = null;
        this.authorities = authorities;
        this.authenticated = true;
    }

    @Override
    public Collection<String> getAuthorities() {
        return authorities;
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
        return principal.toString();
    }
}
