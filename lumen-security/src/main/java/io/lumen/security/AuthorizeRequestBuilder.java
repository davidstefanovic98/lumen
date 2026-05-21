package io.lumen.security;

import io.lumen.web.http.HttpMethod;

import java.util.List;

public class AuthorizeRequestBuilder {
    private final List<AuthorizationRule> rules;

    public AuthorizeRequestBuilder(List<AuthorizationRule> rules) {
        this.rules = rules;
    }

    public AntMatcherConfig antMatchers(String... patterns) {
        return new AntMatcherConfig(this, patterns, null);
    }

    public AntMatcherConfig antMatchers(HttpMethod httpMethod, String... patterns) {
        return new AntMatcherConfig(this, patterns, httpMethod);
    }

    public RegexMatcherConfig regexMatchers(String... regexes) {
        return new RegexMatcherConfig(this, regexes);
    }

    public AnyRequestConfigurer anyRequest() {
        return new AnyRequestConfigurer(this.rules);
    }

    public static class AntMatcherConfig {
        private final AuthorizeRequestBuilder builder;
        private final String[] patterns;
        private final HttpMethod httpMethod;

        public AntMatcherConfig(AuthorizeRequestBuilder builder, String[] patterns, HttpMethod httpMethod) {
            this.builder = builder;
            this.patterns = patterns;
            this.httpMethod = httpMethod;
        }

        public AuthorizeRequestBuilder hasRole(String role) {
            for (String pattern : patterns) {
                RequestMatcher matcher = httpMethod != null
                        ? new AntPathRequestMatcher(pattern, httpMethod)
                        : new AntPathRequestMatcher(pattern);
                builder.rules.add(new AuthorizationRule(matcher, role));
            }
            return builder;
        }

        public AuthorizeRequestBuilder permitAll() {
            for (String pattern : patterns) {
                RequestMatcher matcher = httpMethod != null
                        ? new AntPathRequestMatcher(pattern, httpMethod)
                        : new AntPathRequestMatcher(pattern);
                builder.rules.add(new AuthorizationRule(matcher, "PERMIT_ALL"));
            }
            return builder;
        }

        public AuthorizeRequestBuilder authenticated() {
            for (String pattern : patterns) {
                RequestMatcher matcher = httpMethod != null
                        ? new AntPathRequestMatcher(pattern, httpMethod)
                        : new AntPathRequestMatcher(pattern);
                builder.rules.add(new AuthorizationRule(matcher, "IS_AUTHENTICATED"));
            }
            return builder;
        }
    }

    public static class RegexMatcherConfig {
        private final AuthorizeRequestBuilder builder;
        private final String[] regexes;

        public RegexMatcherConfig(AuthorizeRequestBuilder builder, String[] regexes) {
            this.builder = builder;
            this.regexes = regexes;
        }

        public AuthorizeRequestBuilder hasRole(String role) {
            for (String regex : regexes) {
                builder.rules.add(new AuthorizationRule(new RegexRequestMatcher(regex), role));
            }
            return builder;
        }
    }

    public static class AnyRequestConfigurer {
        private final List<AuthorizationRule> rules;

        public AnyRequestConfigurer(List<AuthorizationRule> rules) {
            this.rules = rules;
        }

        public void authenticated() {
            rules.add(new AuthorizationRule(new AnyPathRequestMatcher(), "IS_AUTHENTICATED"));
        }

        public void permitAll() {
            rules.add(new AuthorizationRule(new AnyPathRequestMatcher(), "PERMIT_ALL"));
        }
    }
}
