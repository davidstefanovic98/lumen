package io.lumen.security.authentication;

import io.lumen.security.authority.GrantedAuthority;

import java.util.Collection;

public interface UserDetails {

    String getUsername();

    String getPassword();

    Collection<? extends GrantedAuthority> getAuthorities();

    boolean isEnabled();

    // Add other user-related methods as needed
}
