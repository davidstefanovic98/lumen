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
 * <p>Register by adding your contributor as an external light on the {@code LightContainer}
 * during {@code LumenModule.init()}, keyed by your contributor's own concrete class so multiple
 * contributors of different types can coexist without colliding on the same container key:
 * <pre>{@code
 * container.registerExternalInstance(MyRuleContributor.class, new MyRuleContributor());
 * }</pre>
 * {@code HttpSecurity} gathers all {@code SecurityRuleContributor} lights via constructor
 * injection, scoped to whichever container built it.
 */
public interface SecurityRuleContributor {
    List<AuthorizationRule> getRules();
}