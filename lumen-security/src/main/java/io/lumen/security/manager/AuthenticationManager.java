package io.lumen.security.manager;

import io.lumen.security.authentication.Authentication;

public interface AuthenticationManager {

    Authentication authenticate(Authentication authentication);
}
