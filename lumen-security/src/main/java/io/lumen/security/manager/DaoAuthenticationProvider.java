package io.lumen.security.manager;

import io.lumen.context.annotation.Component;
import io.lumen.context.annotation.Lazy;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UserDetails;
import io.lumen.security.authentication.UserDetailsService;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.crypto.PasswordEncoder;
import io.lumen.security.exception.BadCredentialsException;

@Component
public class DaoAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public DaoAuthenticationProvider(@Lazy UserDetailsService userDetailsService, @Lazy PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        UserDetails user = userDetailsService.loadUserByUsername(username);

        // Basic check until PasswordEncoder is implemented
        if (passwordEncoder.matches(password, user.getPassword())) {
            return new UsernamePasswordAuthenticationToken(
                    user,
                    user.getAuthorities()
            );
        }

        throw new BadCredentialsException("Invalid password");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
