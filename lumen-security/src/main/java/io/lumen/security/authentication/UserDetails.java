package io.lumen.security.authentication;

import java.util.Collection;

public interface UserDetails {

    String getUsername();

    String getPassword();

    Collection<String> getAuthorities();

    boolean isEnabled();

    // Add other user-related methods as needed
}
