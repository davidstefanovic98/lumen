package io.lumen.mail;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

/**
 * No-op {@link MailSender} used when no SMTP host is configured.
 * Logs at WARN level instead of sending so the app starts cleanly in development.
 */
public class NoOpMailSender implements MailSender {

    private static final Logger logger = LoggerFactory.getLogger(NoOpMailSender.class);

    @Override
    public void send(SimpleMailMessage... messages) throws MailException {
        for (SimpleMailMessage msg : messages) {
            logger.warn("[NoOpMailSender] Mail not sent (lumen.mail.host not configured) " +
                    "→ to={} subject={}",
                    msg.getTo() != null ? String.join(", ", msg.getTo()) : "?",
                    msg.getSubject());
        }
    }
}