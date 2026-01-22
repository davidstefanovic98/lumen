package io.lumen.security.manager;

import io.lumen.security.authentication.Authentication;

public interface AuthenticationProvider {

    Authentication authenticate(Authentication authentication);

    boolean supports(Class<?> authenticationClass);
}
