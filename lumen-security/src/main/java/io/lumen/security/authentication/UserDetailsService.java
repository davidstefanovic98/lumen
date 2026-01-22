package io.lumen.security.authentication;

import io.lumen.security.exception.UsernameNotFoundException;

public interface UserDetailsService {

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
