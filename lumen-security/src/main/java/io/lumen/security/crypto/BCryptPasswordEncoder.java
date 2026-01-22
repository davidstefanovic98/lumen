package io.lumen.security.crypto;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordEncoder implements PasswordEncoder{
    private final int logRounds;

    public BCryptPasswordEncoder() {
        this(10); // Default strength
    }

    public BCryptPasswordEncoder(int logRounds) {
        this.logRounds = logRounds;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        // Generates a salt and hashes the password
        return BCrypt.hashpw(rawPassword.toString(), BCrypt.gensalt(logRounds));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // BCrypt.checkpw extracts the salt from the encodedPassword automatically
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
}
