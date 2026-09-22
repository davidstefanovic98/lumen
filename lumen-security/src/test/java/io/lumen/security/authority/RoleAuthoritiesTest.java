package io.lumen.security.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleAuthoritiesTest {

    @Test
    void addsRolePrefix_whenMissing() {
        assertEquals("ROLE_ADMIN", RoleAuthorities.normalize("ADMIN"));
    }

    @Test
    void doesNotDoublePrefix_whenAlreadyPresent() {
        assertEquals("ROLE_ADMIN", RoleAuthorities.normalize("ROLE_ADMIN"));
    }
}