package io.lumen.security;

import io.lumen.context.annotation.Configuration;
import io.lumen.core.annotation.Light;
import io.lumen.data.User;
import io.lumen.security.authentication.UserDetailsService;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.crypto.BCryptPasswordEncoder;
import io.lumen.security.crypto.PasswordEncoder;
import io.lumen.security.manager.AuthenticationManager;
import io.lumen.security.manager.InMemoryUserDetailsManager;
import io.lumen.web.http.HttpMethod;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Light
    public SecurityFilterChain securityFilterChain(AuthenticationManager authManager) {
        return HttpSecurity
                .builder(authManager)
                .authorizeRequests(auth -> auth
                        .antMatchers(HttpMethod.GET.name(), "/admin/**").hasRole("ROLE_ADMIN")
                        .antMatchers("/api/**").hasRole("ROLE_USER")
                )
                .formLogin()
                .build();
    }

    @Light
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Light
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        manager.createUser(
                new User("admin", "", encoder.encode("Test123!"),
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), "admin"));
        manager.createUser(
                new User(
                        "user",
                        "",
                        encoder.encode("Test123!"),
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        "user"));

        return manager;
    }
}
