package io.lumen.security.manager;

import io.lumen.context.annotation.Component;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.exception.AuthenticationException;

import java.util.List;

@Component
public class ProviderManager implements AuthenticationManager {
    private static final Logger logger = LoggerFactory.getLogger(ProviderManager.class);
    private final List<AuthenticationProvider> providers;

    public ProviderManager(List<AuthenticationProvider> providers) {
        this.providers = providers;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(authentication.getClass())) {
                return provider.authenticate(authentication);
            }
        }
        logger.error("No provider found for authentication type: {}", authentication.getClass().getName());
        throw new AuthenticationException("No provider found for this authentication type");
    }
}
