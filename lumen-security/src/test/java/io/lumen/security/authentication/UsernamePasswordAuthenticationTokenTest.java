package io.lumen.security.authentication;

import io.lumen.security.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UsernamePasswordAuthenticationTokenTest {

    @Test
    void getName_returnsUsername_whenPrincipalIsUserDetails() {
        UserDetails user = new StubUserDetails("user@example.com");
        var token = new UsernamePasswordAuthenticationToken(user, null, List.of());
        assertEquals("user@example.com", token.getName());
    }

    @Test
    void getName_returnsToString_whenPrincipalIsString() {
        var token = new UsernamePasswordAuthenticationToken("alice", null, List.of());
        assertEquals("alice", token.getName());
    }

    @Test
    void getName_returnsEmpty_whenPrincipalIsNull() {
        var token = new UsernamePasswordAuthenticationToken(null, null);
        assertEquals("", token.getName());
    }

    private static class StubUserDetails implements UserDetails {
        private final String username;

        StubUserDetails(String username) {
            this.username = username;
        }

        @Override public String getUsername() { return username; }
        @Override public String getPassword() { return ""; }
        @Override public java.util.Collection<? extends io.lumen.security.authority.GrantedAuthority> getAuthorities() { return List.of(); }
        @Override public boolean isEnabled() { return true; }
    }
}