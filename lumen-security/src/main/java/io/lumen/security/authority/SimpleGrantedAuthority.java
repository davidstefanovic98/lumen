package io.lumen.security.authority;

public record SimpleGrantedAuthority(String role) implements GrantedAuthority {
    @Override
    public String getAuthority() {
        return role;
    }
}
