package io.lumen.security.context;

import io.lumen.security.authentication.Authentication;

public interface SecurityContext {
    Authentication getAuthentication();
    void setAuthentication(Authentication authentication);
}
