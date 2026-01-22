package io.lumen.security;

import io.lumen.context.annotation.Configuration;
import io.lumen.context.annotation.Light;
import io.lumen.data.User;
import io.lumen.security.authentication.UserDetailsService;
import io.lumen.security.crypto.BCryptPasswordEncoder;
import io.lumen.security.crypto.PasswordEncoder;
import io.lumen.security.manager.AuthenticationManager;
import io.lumen.security.manager.InMemoryUserDetailsManager;
import io.lumen.security.repository.HttpSessionSecurityContextRepository;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Light
    public SecurityFilterChain securityFilterChain(AuthenticationManager authManager) {
        List<AuthorizationRule> rules = List.of(
                new AuthorizationRule(new AntPathRequestMatcher("/admin/**"), "ROLE_ADMIN"),
                new AuthorizationRule(new AntPathRequestMatcher("/api/**"), "ROLE_USER")
        );

        return new SecurityFilterChain("/**", List.of(
                new SecurityContextPersistenceFilter(new HttpSessionSecurityContextRepository()),
                new DefaultLoginPageGeneratingFilter(),
                new UsernamePasswordAuthenticationFilter(authManager),
                new AuthorizationFilter(rules)
        ));
    }

    @Light
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Light
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        manager.createUser(new User("admin", "", encoder.encode("admin123"), List.of("ROLE_ADMIN"), "admin"));

        manager.createUser(new User("user", "", encoder.encode("user123"), List.of("ROLE_USER"), "user"));

        return manager;
    }
}
