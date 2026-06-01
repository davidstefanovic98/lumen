package io.lumen.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when one or more messages could not be sent. Carries a map of the
 * failed messages so callers can retry or log exactly which ones failed.
 */
public class MailSendException extends MailException {

    private final Map<Object, Exception> failedMessages;

    public MailSendException(String message, Throwable cause) {
        super(message, cause);
        this.failedMessages = new LinkedHashMap<>();
    }

    public MailSendException(Map<Object, Exception> failedMessages) {
        super(buildMessage(failedMessages));
        this.failedMessages = failedMessages;
    }

    /** Messages that failed, keyed by the original message object. */
    public Map<Object, Exception> getFailedMessages() {
        return failedMessages;
    }

    private static String buildMessage(Map<Object, Exception> failed) {
        StringBuilder sb = new StringBuilder("Mail send failed for ")
                .append(failed.size()).append(" message(s):");
        failed.forEach((msg, ex) ->
                sb.append("\n  ").append(msg).append(" → ").append(ex.getMessage()));
        return sb.toString();
    }
}