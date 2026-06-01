package io.lumen.mail;

/** Thrown when authentication against the mail server fails. */
public class MailAuthenticationException extends MailException {

    public MailAuthenticationException(String message) {
        super(message);
    }

    public MailAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}