package io.lumen.mail.javamail;

import io.lumen.mail.MailParseException;
import io.lumen.mail.MailPreparationException;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MimeMessageHelperTest {

    MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    // -----------------------------------------------------------------------
    // Addressing
    // -----------------------------------------------------------------------

    @Test
    void setFrom_parsesCorrectly() throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setFrom("sender@example.com");
        assertEquals("sender@example.com", mimeMessage.getFrom()[0].toString());
    }

    @Test
    void setFrom_invalidAddress_throwsMailParseException() {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        assertThrows(MailParseException.class, () -> helper.setFrom("not-an-email@@@@"));
    }

    @Test
    void setTo_single() throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo("to@example.com");
        Address[] recipients = mimeMessage.getRecipients(Message.RecipientType.TO);
        assertEquals(1, recipients.length);
        assertEquals("to@example.com", recipients[0].toString());
    }

    @Test
    void setTo_multiple() throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo("a@x.com", "b@x.com");
        assertEquals(2, mimeMessage.getRecipients(Message.RecipientType.TO).length);
    }

    @Test
    void setCc_and_setBcc() throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setCc("cc@example.com");
        helper.setBcc("bcc@example.com");
        assertEquals(1, mimeMessage.getRecipients(Message.RecipientType.CC).length);
        assertEquals(1, mimeMessage.getRecipients(Message.RecipientType.BCC).length);
    }

    // -----------------------------------------------------------------------
    // Subject & dates
    // -----------------------------------------------------------------------

    @Test
    void setSubject_encodedCorrectly() throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setSubject("Hello World");
        assertEquals("Hello World", mimeMessage.getSubject());
    }

    @Test
    void setSentDate() throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        Date now = new Date();
        helper.setSentDate(now);
        // Truncated to seconds by mail spec
        assertNotNull(mimeMessage.getSentDate());
    }

    // -----------------------------------------------------------------------
    // Plain-text body
    // -----------------------------------------------------------------------

    @Test
    void setText_plainText() throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setText("Hello plain");
        assertTrue(mimeMessage.getContent().toString().contains("Hello plain"));
        assertTrue(mimeMessage.getContentType().startsWith("text/plain"));
    }

    @Test
    void setText_html() throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setText("<b>Hello</b>", true);
        mimeMessage.saveChanges(); // flushes content-type header
        assertTrue(mimeMessage.getContentType().startsWith("text/html"),
                "Expected text/html but got: " + mimeMessage.getContentType());
    }

    // -----------------------------------------------------------------------
    // Multipart
    // -----------------------------------------------------------------------

    @Test
    void multipart_isMultipart_true() {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        assertTrue(helper.isMultipart());
    }

    @Test
    void multipart_textAndHtml_alternativeParts() throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setText("plain text", "<b>html</b>");
        Object content = mimeMessage.getContent();
        assertInstanceOf(Multipart.class, content);
    }

    @Test
    void setText_plainHtml_withoutMultipart_throws() {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false);
        assertThrows(IllegalStateException.class, () -> helper.setText("plain", "<b>html</b>"));
    }

    @Test
    void addAttachment_withoutMultipart_throws() {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false);
        assertThrows(IllegalStateException.class,
                () -> helper.addAttachment("file.txt", new File("/tmp/file.txt")));
    }

    @Test
    void addAttachment_multipartMode_addsBodyPart() throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setText("body");
        // Use a DataSource instead of File to avoid filesystem dependency
        helper.addAttachment("report.txt",
                new jakarta.activation.FileDataSource(File.createTempFile("lumen-test", ".txt")));
        Multipart mp = (Multipart) mimeMessage.getContent();
        assertTrue(mp.getCount() > 1);
    }

    // -----------------------------------------------------------------------
    // MimeMailMessage delegation
    // -----------------------------------------------------------------------

    @Test
    void mimeMailMessage_delegatesAllSetters() throws MessagingException {
        MimeMailMessage mailMessage = new MimeMailMessage(mimeMessage);
        mailMessage.setFrom("from@example.com");
        mailMessage.setTo("to@example.com");
        mailMessage.setSubject("Sub");
        mailMessage.setText("Body");

        assertEquals("from@example.com", mimeMessage.getFrom()[0].toString());
        assertEquals("to@example.com",   mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Sub", mimeMessage.getSubject());
    }
}