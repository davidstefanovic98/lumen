package io.lumen.security.authentication;

import java.util.Collection;

public interface Authentication {

    Collection<String> getAuthorities();

    Object getCredentials();

    Object getPrincipal();

    boolean isAuthenticated();

    void setAuthenticated(boolean isAuthenticated);

    String getName();
}
