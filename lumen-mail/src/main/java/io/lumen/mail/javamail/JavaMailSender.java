package io.lumen.mail.javamail;

import io.lumen.mail.MailException;
import io.lumen.mail.MailSender;
import jakarta.mail.internet.MimeMessage;

/**
 * Extended {@link MailSender} that supports MIME messages.
 * Use {@link MimeMessageHelper} to conveniently populate a {@link MimeMessage}.
 */
public interface JavaMailSender extends MailSender {

    /** Create a new {@link MimeMessage} using the underlying mail session. */
    MimeMessage createMimeMessage();

    /**
     * Send one or more pre-built MIME messages.
     * Failed messages are collected and thrown as a single {@link io.lumen.mail.MailSendException}.
     */
    void send(MimeMessage... mimeMessages) throws MailException;

    /**
     * Send using a callback that populates the message just before sending.
     * The {@link MimeMessage} is created for you — the preparator only needs to fill it in.
     */
    default void send(MimeMessagePreparator preparator) throws MailException {
        send(new MimeMessagePreparator[]{ preparator });
    }

    void send(MimeMessagePreparator... preparators) throws MailException;
}