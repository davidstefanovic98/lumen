package io.lumen.security;

import io.lumen.security.manager.AuthenticationManager;
import io.lumen.security.repository.HttpSessionSecurityContextRepository;

import java.util.ArrayList;
import java.util.List;

public class HttpSecurity {
    private HttpSecurity() {}

    public static Builder builder(AuthenticationManager authManager) {
        return new Builder(authManager);
    }

    public static class Builder {
        private final AuthenticationManager authManager;
        private final List<AuthorizationRule> rules = new ArrayList<>();
        private boolean formLoginEnabled = false;
        private boolean logoutEnabled = true;

        protected Builder(AuthenticationManager authManager) {
            this.authManager = authManager;
        }

        public Builder authorizeRequests(java.util.function.Consumer<AuthorizeRequestBuilder> consumer) {
            AuthorizeRequestBuilder authBuilder = new AuthorizeRequestBuilder(this.rules);
            consumer.accept(authBuilder);
            return this;
        }

        public Builder formLogin() {
            this.formLoginEnabled = true;
            return this;
        }

        public Builder logout() {
            this.logoutEnabled = true;
            return this;
        }

        public SecurityFilterChain build() {
            List<SecuritySubFilter> filters = new ArrayList<>();
            filters.add(new SecurityContextPersistenceFilter(new HttpSessionSecurityContextRepository()));

            if (formLoginEnabled) {
                rules.addFirst(new AuthorizationRule(new AntPathRequestMatcher("/login"), "PERMIT_ALL"));
                filters.add(new DefaultLoginPageGeneratingFilter());
            }
            filters.add(new UsernamePasswordAuthenticationFilter(authManager));

            if (logoutEnabled) {
                filters.add(new LogoutFilter());
                rules.addFirst(new AuthorizationRule(new AntPathRequestMatcher("/logout"), "PERMIT_ALL"));
            }

            filters.add(new ExceptionTranslationFilter());
            filters.add(new AuthorizationFilter(rules));

            return new SecurityFilterChain("/**", filters);
        }

        void addRule(AuthorizationRule rule) {
            this.rules.add(rule);
        }
    }
}
