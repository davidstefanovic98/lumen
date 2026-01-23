package io.lumen.data;

import io.lumen.security.authentication.UserDetails;
import io.lumen.security.authority.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class User implements UserDetails {
    private String name;
    private String email;
    private String password;
    private List<GrantedAuthority> roles;
    private String username;

    public User(String name, String email, String password, List<GrantedAuthority> roles, String username) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
