package io.lumen.mail.javamail;

import io.lumen.mail.MailParseException;
import io.lumen.mail.MailPreparationException;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Helper for populating a {@link MimeMessage}.
 *
 * Supports plain text, HTML, and multipart messages with attachments.
 * Get one from {@link JavaMailSender#createMimeMessage()}, then wrap it here:
 * <pre>
 *   MimeMessage msg = sender.createMimeMessage();
 *   MimeMessageHelper helper = new MimeMessageHelper(msg, true);  // true = multipart
 *   helper.setTo("user@example.com");
 *   helper.setSubject("Hello");
 *   helper.setText("plain body", "&lt;b&gt;html body&lt;/b&gt;", true);
 *   helper.addAttachment("file.pdf", new File("/tmp/file.pdf"));
 *   sender.send(msg);
 * </pre>
 */
public class MimeMessageHelper {

    public static final int MULTIPART_MODE_NO       = 0;
    public static final int MULTIPART_MODE_MIXED     = 1;
    public static final int MULTIPART_MODE_RELATED   = 2;
    public static final int MULTIPART_MODE_MIXED_RELATED = 3;

    private final MimeMessage mimeMessage;
    private final String encoding;
    private Multipart rootMultipart;
    private Multipart mimeMultipart;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** Create a helper for a plain (non-multipart) message in UTF-8. */
    public MimeMessageHelper(MimeMessage mimeMessage) {
        this(mimeMessage, false);
    }

    /** @param multipart true to prepare a multipart message (needed for attachments or HTML+text). */
    public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart) {
        this(mimeMessage, multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO,
                StandardCharsets.UTF_8.name());
    }

    public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart, String encoding) {
        this(mimeMessage, multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO, encoding);
    }

    public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode, String encoding) {
        this.mimeMessage = mimeMessage;
        this.encoding    = encoding != null ? encoding : StandardCharsets.UTF_8.name();
        if (multipartMode != MULTIPART_MODE_NO) {
            try {
                createMultipartParts(multipartMode);
            } catch (MessagingException e) {
                throw new MailPreparationException("Could not create multipart message", e);
            }
        }
    }

    private void createMultipartParts(int mode) throws MessagingException {
        if (mode == MULTIPART_MODE_MIXED || mode == MULTIPART_MODE_MIXED_RELATED) {
            rootMultipart = new MimeMultipart("mixed");
            mimeMessage.setContent(rootMultipart);
        }
        if (mode == MULTIPART_MODE_RELATED || mode == MULTIPART_MODE_MIXED_RELATED) {
            mimeMultipart = new MimeMultipart("related");
            if (rootMultipart != null) {
                MimeBodyPart relatedPart = new MimeBodyPart();
                relatedPart.setContent(mimeMultipart);
                rootMultipart.addBodyPart(relatedPart);
            } else {
                mimeMessage.setContent(mimeMultipart);
            }
        } else {
            mimeMultipart = rootMultipart;
        }
    }

    public MimeMessage getMimeMessage() { return mimeMessage; }
    public boolean isMultipart()        { return rootMultipart != null; }
    public String getEncoding()         { return encoding; }

    // -----------------------------------------------------------------------
    // Addressing
    // -----------------------------------------------------------------------

    public void setFrom(String from) throws MailParseException {
        try {
            mimeMessage.setFrom(parseAddress(from));
        } catch (MessagingException e) {
            throw new MailParseException("Invalid from address: " + from, e);
        }
    }

    public void setReplyTo(String replyTo) throws MailParseException {
        try {
            mimeMessage.setReplyTo(new InternetAddress[]{ parseAddress(replyTo) });
        } catch (MessagingException e) {
            throw new MailParseException("Invalid reply-to address: " + replyTo, e);
        }
    }

    public void setTo(String to) throws MailParseException {
        setRecipients(Message.RecipientType.TO, to);
    }

    public void setTo(String... to) throws MailParseException {
        setRecipients(Message.RecipientType.TO, to);
    }

    public void setCc(String cc) throws MailParseException {
        setRecipients(Message.RecipientType.CC, cc);
    }

    public void setCc(String... cc) throws MailParseException {
        setRecipients(Message.RecipientType.CC, cc);
    }

    public void setBcc(String bcc) throws MailParseException {
        setRecipients(Message.RecipientType.BCC, bcc);
    }

    public void setBcc(String... bcc) throws MailParseException {
        setRecipients(Message.RecipientType.BCC, bcc);
    }

    private void setRecipients(Message.RecipientType type, String... addresses) throws MailParseException {
        try {
            InternetAddress[] parsed = new InternetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++) parsed[i] = parseAddress(addresses[i]);
            mimeMessage.setRecipients(type, parsed);
        } catch (MessagingException e) {
            throw new MailParseException("Invalid address in " + type + ": " + String.join(", ", addresses), e);
        }
    }

    public void setSentDate(Date sentDate) {
        try { mimeMessage.setSentDate(sentDate); }
        catch (MessagingException e) { throw new MailPreparationException("Could not set sent date", e); }
    }

    // -----------------------------------------------------------------------
    // Subject & body
    // -----------------------------------------------------------------------

    public void setSubject(String subject) {
        try { mimeMessage.setSubject(subject, encoding); }
        catch (MessagingException e) { throw new MailPreparationException("Could not set subject", e); }
    }

    /** Set plain-text body. For HTML or multipart use {@link #setText(String, boolean)}. */
    public void setText(String text) {
        setText(text, false);
    }

    public void setText(String text, boolean html) {
        try {
            String contentType = (html ? "text/html" : "text/plain") + ";charset=" + encoding;
            if (isMultipart()) {
                MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(text, contentType);
                mimeMultipart.addBodyPart(bodyPart, 0);
            } else {
                mimeMessage.setContent(text, contentType);
            }
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not set text body", e);
        }
    }

    /**
     * Set both a plain-text and an HTML alternative body in a multipart/alternative part.
     * Requires the helper to be created with {@code multipart = true}.
     */
    public void setText(String plainText, String htmlText) {
        if (!isMultipart()) throw new IllegalStateException(
                "setText(plain, html) requires multipart mode — pass true to the constructor");
        try {
            MimeMultipart alternative = new MimeMultipart("alternative");
            MimeBodyPart plainPart = new MimeBodyPart();
            plainPart.setContent(plainText, "text/plain;charset=" + encoding);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlText, "text/html;charset=" + encoding);
            alternative.addBodyPart(plainPart);
            alternative.addBodyPart(htmlPart);

            MimeBodyPart alternativePart = new MimeBodyPart();
            alternativePart.setContent(alternative);
            mimeMultipart.addBodyPart(alternativePart, 0);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not set alternative text/html body", e);
        }
    }

    // -----------------------------------------------------------------------
    // Attachments & inline resources
    // -----------------------------------------------------------------------

    public void addAttachment(String attachmentFilename, File file) {
        addAttachment(attachmentFilename, new FileDataSource(file));
    }

    public void addAttachment(String attachmentFilename, DataSource dataSource) {
        assertMultipart("addAttachment");
        try {
            MimeBodyPart part = new MimeBodyPart();
            part.setDisposition(MimeBodyPart.ATTACHMENT);
            part.setFileName(attachmentFilename);
            part.setDataHandler(new DataHandler(dataSource));
            rootMultipart.addBodyPart(part);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not add attachment '" + attachmentFilename + "'", e);
        }
    }

    public void addInline(String contentId, File file) {
        addInline(contentId, new FileDataSource(file));
    }

    public void addInline(String contentId, DataSource dataSource) {
        assertMultipart("addInline");
        try {
            MimeBodyPart part = new MimeBodyPart();
            part.setDisposition(MimeBodyPart.INLINE);
            part.setContentID("<" + contentId + ">");
            part.setDataHandler(new DataHandler(dataSource));
            mimeMultipart.addBodyPart(part);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not add inline resource '" + contentId + "'", e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private InternetAddress parseAddress(String address) throws MailParseException {
        try {
            return new InternetAddress(address);
        } catch (AddressException e) {
            throw new MailParseException("Invalid email address: " + address, e);
        }
    }

    private void assertMultipart(String method) {
        if (!isMultipart()) throw new IllegalStateException(
                method + "() requires multipart mode — pass true to the constructor");
    }
}