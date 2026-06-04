package io.lumen.actuator;

import io.lumen.security.AntPathRequestMatcher;
import io.lumen.security.AuthorizationRule;
import io.lumen.security.HttpSecurity;
import io.lumen.security.SecurityRuleContributor;

import java.util.List;

/**
 * Registers default actuator authorization rules into the HTTP security filter chain.
 * Called by {@link LumenActuatorModule} when {@code lumen-security} is on the classpath.
 *
 * <p>Rules added here are appended after user-configured rules, so any explicit
 * {@code antMatchers("/actuator/**").permitAll()} in the app's security config overrides them.
 */
class ActuatorSecurityContributor implements SecurityRuleContributor {

    private final String basePath;

    private ActuatorSecurityContributor(String basePath) {
        this.basePath = basePath;
    }

    static void register(String basePath) {
        HttpSecurity.addRuleContributor(new ActuatorSecurityContributor(basePath));
    }

    @Override
    public List<AuthorizationRule> getRules() {
        return List.of(
                new AuthorizationRule(new AntPathRequestMatcher(basePath + "/health"), "PERMIT_ALL"),
                new AuthorizationRule(new AntPathRequestMatcher(basePath + "/info"),   "PERMIT_ALL"),
                new AuthorizationRule(new AntPathRequestMatcher(basePath + "/**"),     "IS_AUTHENTICATED")
        );
    }
}