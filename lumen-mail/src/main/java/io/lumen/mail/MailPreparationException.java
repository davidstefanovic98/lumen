package io.lumen.mail;

/** Thrown when a mail message cannot be prepared (e.g. template rendering failure). */
public class MailPreparationException extends MailException {

    public MailPreparationException(String message) {
        super(message);
    }

    public MailPreparationException(String message, Throwable cause) {
        super(message, cause);
    }
}