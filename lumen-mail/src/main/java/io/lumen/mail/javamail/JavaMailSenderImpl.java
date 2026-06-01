package io.lumen.mail.javamail;

import io.lumen.mail.MailAuthenticationException;
import io.lumen.mail.MailException;
import io.lumen.mail.MailPreparationException;
import io.lumen.mail.MailSendException;
import io.lumen.mail.SimpleMailMessage;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Full {@link JavaMailSender} implementation backed by Jakarta Mail.
 * <p>
 * Minimal required configuration: {@code host}. Everything else has sensible defaults.
 * Configure via {@code application.properties} — {@link io.lumen.mail.LumenMailModule} constructs
 * and registers this bean when {@code lumen.mail.host} is set.
 */
public class JavaMailSenderImpl implements JavaMailSender {

    private static final Logger logger = LoggerFactory.getLogger(JavaMailSenderImpl.class);

    private static final String DEFAULT_PROTOCOL = "smtp";
    private static final String DEFAULT_ENCODING = "UTF-8";

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String defaultFrom;
    private final String protocol;
    private final String encoding;
    private final boolean auth;
    private final boolean starttls;
    private final Properties extraProperties;

    private volatile Session session;

    public JavaMailSenderImpl(String host, int port, String username, String password,
                              String defaultFrom, boolean auth, boolean starttls) {
        this(host, port, username, password, defaultFrom, auth, starttls,
                DEFAULT_PROTOCOL, DEFAULT_ENCODING, new Properties());
    }

    public JavaMailSenderImpl(String host, int port, String username, String password,
                              String defaultFrom, boolean auth, boolean starttls,
                              String protocol, String encoding, Properties extraProperties) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.defaultFrom = defaultFrom;
        this.auth = auth;
        this.starttls = starttls;
        this.protocol = protocol != null ? protocol : DEFAULT_PROTOCOL;
        this.encoding = encoding != null ? encoding : DEFAULT_ENCODING;
        this.extraProperties = extraProperties != null ? extraProperties : new Properties();
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        MimeMessage[] mimeMessages = new MimeMessage[simpleMessages.length];
        for (int i = 0; i < simpleMessages.length; i++) {
            mimeMessages[i] = toMimeMessage(simpleMessages[i]);
        }
        send(mimeMessages);
    }

    private MimeMessage toMimeMessage(SimpleMailMessage simple) {
        MimeMessage mime = createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, false, encoding);
        try {
            String from = simple.getFrom() != null ? simple.getFrom() : defaultFrom;
            if (from != null) helper.setFrom(from);
            if (simple.getReplyTo() != null)
                helper.setReplyTo(simple.getReplyTo());
            if (simple.getTo() != null)
                helper.setTo(simple.getTo());
            if (simple.getCc() != null)
                helper.setCc(simple.getCc());
            if (simple.getBcc() != null)
                helper.setBcc(simple.getBcc());
            if (simple.getSentDate() != null)
                helper.setSentDate(simple.getSentDate());
            else helper.setSentDate(new Date());
            if (simple.getSubject() != null)
                helper.setSubject(simple.getSubject());
            if (simple.getText() != null)
                helper.setText(simple.getText());
        } catch (Exception e) {
            throw new MailPreparationException("Failed to convert SimpleMailMessage to MimeMessage", e);
        }
        return mime;
    }

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage(getSession());
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        Map<Object, Exception> failures = new LinkedHashMap<>();
        for (MimeMessage msg : mimeMessages) {
            try {
                Transport.send(msg);
                logSent(msg);
            } catch (AuthenticationFailedException e) {
                throw new MailAuthenticationException("SMTP authentication failed for " + host, e);
            } catch (MessagingException e) {
                failures.put(msg, e);
            }
        }
        if (!failures.isEmpty()) {
            throw new MailSendException(failures);
        }
    }

    @Override
    public void send(MimeMessagePreparator... preparators) throws MailException {
        MimeMessage[] messages = new MimeMessage[preparators.length];
        for (int i = 0; i < preparators.length; i++) {
            messages[i] = createMimeMessage();
            try {
                preparators[i].prepare(messages[i]);
            } catch (Exception e) {
                throw new MailPreparationException("MimeMessagePreparator threw an exception", e);
            }
        }
        send(messages);
    }


    private Session getSession() {
        if (session == null) {
            synchronized (this) {
                if (session == null) session = buildSession();
            }
        }
        return session;
    }

    private Session buildSession() {
        Properties props = new Properties(extraProperties);
        props.put("mail." + protocol + ".host", host);
        props.put("mail." + protocol + ".port", String.valueOf(port));
        props.put("mail." + protocol + ".auth", String.valueOf(auth));
        if (starttls)
            props.put("mail." + protocol + ".starttls.enable", "true");

        if (auth && username != null && !username.isBlank()) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }
        return Session.getInstance(props);
    }

    private void logSent(MimeMessage msg) {
        try {
            InternetAddress[] recipients = (InternetAddress[]) msg.getRecipients(MimeMessage.RecipientType.TO);
            String to = recipients != null ? recipients[0].getAddress() : "?";
            logger.debug("Mail sent → {} subject: {}", to, msg.getSubject());
        } catch (MessagingException ignored) {
        }
    }
}