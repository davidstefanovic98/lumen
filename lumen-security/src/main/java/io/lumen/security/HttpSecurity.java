package io.lumen.security;

import io.lumen.security.manager.AuthenticationManager;
import io.lumen.security.repository.HttpSessionSecurityContextRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class HttpSecurity {
    private HttpSecurity() {}

    private static final java.util.concurrent.CopyOnWriteArrayList<SecurityRuleContributor> CONTRIBUTORS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Registers a module-level rule contributor whose rules are appended after the
     * user's own {@code authorizeRequests} rules during {@link Builder#build()}.
     * User rules always take priority — contributors provide overridable defaults.
     */
    public static void addRuleContributor(SecurityRuleContributor contributor) {
        CONTRIBUTORS.add(contributor);
    }

    /** Clears all registered contributors. Intended for use in tests. */
    public static void clearContributors() {
        CONTRIBUTORS.clear();
    }

    public static Builder builder(AuthenticationManager authManager) {
        return new Builder(authManager);
    }

    public static class Builder {
        private final AuthenticationManager authManager;
        private final List<AuthorizationRule> rules = new ArrayList<>();

        private boolean formLoginEnabled = false;
        private String loginPage = "/login";
        private String defaultSuccessUrl = "/";
        private String failureUrl = "/login?error";
        private boolean isCustomLoginPage = false; // Flag to skip default UI

        private boolean logoutEnabled = false;
        private String logoutUrl = "/logout";
        private String logoutSuccessUrl = "/login?logout";

        private final List<SecuritySubFilter> customFilters = new ArrayList<>();

        protected Builder(AuthenticationManager authManager) {
            this.authManager = authManager;
        }

        public Builder authorizeRequests(Consumer<AuthorizeRequestBuilder> consumer) {
            AuthorizeRequestBuilder authBuilder = new AuthorizeRequestBuilder(this.rules);
            consumer.accept(authBuilder);
            return this;
        }

        public Builder formLogin(Consumer<FormLoginConfigurer> consumer) {
            this.formLoginEnabled = true;
            FormLoginConfigurer configurer = new FormLoginConfigurer();
            consumer.accept(configurer);

            this.loginPage = configurer.loginPage;
            this.defaultSuccessUrl = configurer.defaultSuccessUrl;
            this.failureUrl = configurer.failureUrl;
            this.isCustomLoginPage = configurer.customPageSet;
            return this;
        }

        public Builder addFilter(SecuritySubFilter filter) {
            this.customFilters.add(filter);
            return this;
        }

        public Builder logout(Consumer<LogoutConfigurer> consumer) {
            this.logoutEnabled = true;
            LogoutConfigurer configurer = new LogoutConfigurer();
            consumer.accept(configurer);
            this.logoutUrl = configurer.logoutUrl;
            this.logoutSuccessUrl = configurer.logoutSuccessUrl;
            return this;
        }

        public SecurityFilterChain build() {
            List<SecuritySubFilter> filters = new ArrayList<>();

            filters.add(new SecurityContextPersistenceFilter(new HttpSessionSecurityContextRepository()));

            if (formLoginEnabled) {
                rules.addFirst(new AuthorizationRule(new AntPathRequestMatcher(loginPage), "PERMIT_ALL"));

                if (!isCustomLoginPage) {
                    filters.add(new DefaultLoginPageGeneratingFilter());
                }

                filters.add(new UsernamePasswordAuthenticationFilter(authManager, loginPage, defaultSuccessUrl, failureUrl));
            }

            if (logoutEnabled) {
                filters.add(new LogoutFilter(logoutUrl, logoutSuccessUrl));
                rules.addFirst(new AuthorizationRule(new AntPathRequestMatcher(logoutUrl), "PERMIT_ALL"));
            }

            filters.add(new ExceptionTranslationFilter(loginPage, formLoginEnabled));

            List<AuthorizationRule> allRules = new ArrayList<>(rules);
            for (SecurityRuleContributor contributor : CONTRIBUTORS) {
                allRules.addAll(contributor.getRules());
            }
            filters.add(new AuthorizationFilter(allRules));
            filters.addAll(customFilters);
            filters.sort(Comparator.comparingInt(SecuritySubFilter::getOrder));

            return new SecurityFilterChain("/**", filters);
        }

        void addRule(AuthorizationRule rule) {
            this.rules.add(rule);
        }
    }

    public static class FormLoginConfigurer {
        String loginPage = "/login";
        String defaultSuccessUrl = "/";
        String failureUrl = "/login?error";
        boolean customPageSet = false;

        public FormLoginConfigurer loginPage(String path) {
            this.loginPage = path;
            this.customPageSet = true;
            return this;
        }

        public FormLoginConfigurer defaultSuccessUrl(String path) {
            this.defaultSuccessUrl = path;
            return this;
        }

        public FormLoginConfigurer failureUrl(String path) {
            this.failureUrl = path;
            return this;
        }
    }

    public static class LogoutConfigurer {
        String logoutUrl = "/logout";
        String logoutSuccessUrl = "/login?logout";

        public LogoutConfigurer logoutUrl(String url) {
            this.logoutUrl = url;
            return this;
        }

        public LogoutConfigurer logoutSuccessUrl(String url) {
            this.logoutSuccessUrl = url;
            return this;
        }
    }
}