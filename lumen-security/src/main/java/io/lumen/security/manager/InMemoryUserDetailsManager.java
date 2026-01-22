package io.lumen.security.manager;

import io.lumen.context.annotation.Component;
import io.lumen.security.authentication.UserDetails;
import io.lumen.security.authentication.UserDetailsService;
import io.lumen.security.exception.UsernameNotFoundException;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryUserDetailsManager implements UserDetailsService {
    private final Map<String, UserDetails> users = new HashMap<>();

    public void createUser(UserDetails user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (!users.containsKey(username))
            throw new UsernameNotFoundException("User not found");
        return users.get(username);
    }
}
