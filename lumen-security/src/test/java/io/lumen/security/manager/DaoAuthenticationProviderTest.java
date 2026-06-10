package io.lumen.security.manager;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UserDetails;
import io.lumen.security.authentication.UserDetailsService;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.crypto.PasswordEncoder;
import io.lumen.security.exception.BadCredentialsException;
import io.lumen.security.exception.UsernameNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DaoAuthenticationProviderTest {

    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(userDetailsService, passwordEncoder);

    private UserDetails userDetails(String username, String encodedPassword) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getPassword()).thenReturn(encodedPassword);
        when(user.getAuthorities()).thenReturn(List.of());
        return user;
    }

    @Test
    void nullPassword_throwsBadCredentials_notNullPointer() {
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", null);

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        verifyNoInteractions(userDetailsService, passwordEncoder);
    }

    @Test
    void nullUsername_throwsBadCredentials() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        when(auth.getCredentials()).thenReturn("secret");

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        verifyNoInteractions(userDetailsService, passwordEncoder);
    }

    @Test
    void validCredentials_returnsAuthenticatedToken() {
        UserDetails user = userDetails("alice", "encoded-secret");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

        Authentication auth = new UsernamePasswordAuthenticationToken("alice", "secret");
        Authentication result = provider.authenticate(auth);

        assertTrue(result.isAuthenticated());
        assertEquals(user, result.getPrincipal());
    }

    @Test
    void wrongPassword_throwsBadCredentials() {
        UserDetails user = userDetails("alice", "encoded-secret");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded-secret")).thenReturn(false);

        Authentication auth = new UsernamePasswordAuthenticationToken("alice", "wrong");

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
    }

    @Test
    void unknownUsername_throwsUsernameNotFound() {
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("User not found"));

        Authentication auth = new UsernamePasswordAuthenticationToken("ghost", "secret");

        assertThrows(UsernameNotFoundException.class, () -> provider.authenticate(auth));
    }
}