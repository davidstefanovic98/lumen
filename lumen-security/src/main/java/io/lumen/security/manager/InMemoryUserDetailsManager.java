package io.lumen.security.manager;

import io.lumen.context.annotation.Component;
import io.lumen.security.authentication.UserDetails;
import io.lumen.security.authentication.UserDetailsService;
import io.lumen.security.exception.UsernameNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryUserDetailsManager implements UserDetailsService {
    private final Map<String, UserDetails> users = new ConcurrentHashMap<>();

    public void createUser(UserDetails user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserDetails user = users.get(username);
        if (user == null)
            throw new UsernameNotFoundException("User not found");
        return user;
    }
}
