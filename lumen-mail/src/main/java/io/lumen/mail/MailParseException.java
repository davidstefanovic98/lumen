package io.lumen.mail;

/** Thrown when a mail message cannot be parsed (e.g. invalid address format). */
public class MailParseException extends MailException {

    public MailParseException(String message) {
        super(message);
    }

    public MailParseException(String message, Throwable cause) {
        super(message, cause);
    }
}