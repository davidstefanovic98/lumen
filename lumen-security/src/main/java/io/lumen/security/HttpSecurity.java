package io.lumen.security;

import io.lumen.context.annotation.Scope;
import io.lumen.core.component.ScopeType;
import io.lumen.security.manager.AuthenticationManager;
import io.lumen.security.repository.HttpSessionSecurityContextRepository;
import io.lumen.security.repository.SecurityContextRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent HTTP security configurer
 *
 * <p>Prototype-scoped: each time it is injected (e.g. as a parameter of a {@code @Light}
 * {@code SecurityFilterChain} factory method) the container builds a fresh instance, with its
 * own {@link AuthenticationManager} and {@link SecurityRuleContributor} lights resolved from
 * that same container. This is what scopes contributed rules to a single application context —
 * there is no shared static state, so nothing can leak across contexts or test cases.
 */
@Scope(ScopeType.PROTOTYPE)
public class HttpSecurity {

    private final AuthenticationManager authManager;
    private final List<SecurityRuleContributor> contributors;
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

    public HttpSecurity(AuthenticationManager authManager, List<SecurityRuleContributor> contributors) {
        this.authManager = authManager;
        this.contributors = contributors;
    }

    public HttpSecurity authorizeRequests(Consumer<AuthorizeRequestBuilder> consumer) {
        AuthorizeRequestBuilder authBuilder = new AuthorizeRequestBuilder(this.rules);
        consumer.accept(authBuilder);
        return this;
    }

    public HttpSecurity formLogin(Consumer<FormLoginConfigurer> consumer) {
        this.formLoginEnabled = true;
        FormLoginConfigurer configurer = new FormLoginConfigurer();
        consumer.accept(configurer);

        this.loginPage = configurer.loginPage;
        this.defaultSuccessUrl = configurer.defaultSuccessUrl;
        this.failureUrl = configurer.failureUrl;
        this.isCustomLoginPage = configurer.customPageSet;
        return this;
    }

    public HttpSecurity addFilter(SecuritySubFilter filter) {
        this.customFilters.add(filter);
        return this;
    }

    public HttpSecurity logout(Consumer<LogoutConfigurer> consumer) {
        this.logoutEnabled = true;
        LogoutConfigurer configurer = new LogoutConfigurer();
        consumer.accept(configurer);
        this.logoutUrl = configurer.logoutUrl;
        this.logoutSuccessUrl = configurer.logoutSuccessUrl;
        return this;
    }

    public SecurityFilterChain build() {
        List<SecuritySubFilter> filters = new ArrayList<>();

        SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
        filters.add(new SecurityContextPersistenceFilter(securityContextRepository));

        if (formLoginEnabled) {
            rules.addFirst(new AuthorizationRule(new AntPathRequestMatcher(loginPage), "PERMIT_ALL"));

            if (!isCustomLoginPage) {
                filters.add(new DefaultLoginPageGeneratingFilter());
            }

            filters.add(new UsernamePasswordAuthenticationFilter(
                    authManager, securityContextRepository, loginPage, defaultSuccessUrl, failureUrl));
        }

        if (logoutEnabled) {
            filters.add(new LogoutFilter(logoutUrl, logoutSuccessUrl));
            rules.addFirst(new AuthorizationRule(new AntPathRequestMatcher(logoutUrl), "PERMIT_ALL"));
        }

        filters.add(new ExceptionTranslationFilter(loginPage, formLoginEnabled));

        List<AuthorizationRule> allRules = new ArrayList<>(rules);
        for (SecurityRuleContributor contributor : contributors) {
            allRules.addAll(contributor.getRules());
        }
        filters.add(new AuthorizationFilter(allRules));
        filters.addAll(customFilters);
        filters.sort(Comparator.comparingInt(SecuritySubFilter::getOrder));

        return new SecurityFilterChain("/**", filters);
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