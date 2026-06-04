package io.lumen.security;

import java.util.List;

/**
 * SPI for modules that need to register default {@link AuthorizationRule}s into the
 * {@link HttpSecurity} filter chain without requiring the user to configure them manually.
 *
 * <p>Rules contributed here are appended AFTER the user's own rules, so any explicit
 * rule in the user's {@code authorizeRequests(...)} block takes priority and can override
 * module-provided defaults.
 *
 * <p>Register via {@link HttpSecurity#addRuleContributor(SecurityRuleContributor)}.
 */
public interface SecurityRuleContributor {
    List<AuthorizationRule> getRules();
}