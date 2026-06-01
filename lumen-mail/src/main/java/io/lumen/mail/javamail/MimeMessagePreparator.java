package io.lumen.mail.javamail;

import jakarta.mail.internet.MimeMessage;

/**
 * Callback for preparing a {@link MimeMessage} before it is sent.
 * Typically used with {@link JavaMailSender#send(MimeMessagePreparator...)}.
 */
@FunctionalInterface
public interface MimeMessagePreparator {
    void prepare(MimeMessage mimeMessage) throws Exception;
}