package io.lumen.security.authentication;

import io.lumen.security.authority.GrantedAuthority;

import java.util.Collection;

public interface Authentication {

    Collection<? extends GrantedAuthority> getAuthorities();

    Object getCredentials();

    Object getPrincipal();

    boolean isAuthenticated();

    void setAuthenticated(boolean isAuthenticated);

    String getName();
}
