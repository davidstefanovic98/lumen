package io.lumen.security.context;

import io.lumen.security.authentication.Authentication;

public class DefaultSecurityContext implements SecurityContext {

    private Authentication authentication;

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
}
