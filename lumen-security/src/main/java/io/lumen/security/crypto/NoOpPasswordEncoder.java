package io.lumen.security.crypto;

public class NoOpPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // Just a simple string comparison
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return rawPassword.toString().equals(encodedPassword);
    }
}
