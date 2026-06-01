package io.lumen.mail.javamail;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.lumen.mail.MailSendException;
import io.lumen.mail.SimpleMailMessage;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

class JavaMailSenderImplTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(com.icegreen.greenmail.configuration.GreenMailConfiguration
                    .aConfig().withUser("from@example.com", "password"));

    JavaMailSenderImpl sender;

    @BeforeEach
    void setUp() {
        sender = new JavaMailSenderImpl(
                "localhost",
                ServerSetupTest.SMTP.getPort(),
                "from@example.com",
                "password",
                "from@example.com",
                true,
                false  // no STARTTLS in tests
        );
    }

    // -----------------------------------------------------------------------
    // SimpleMailMessage
    // -----------------------------------------------------------------------

    @Test
    void send_simpleMailMessage_delivered() throws Exception {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("from@example.com");
        msg.setTo("to@example.com");
        msg.setSubject("Hello");
        msg.setText("World");

        sender.send(msg);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("Hello", received[0].getSubject());
        assertEquals("World", GreenMailUtil.getBody(received[0]).trim());
    }

    @Test
    void send_batch_simpleMessages_allDelivered() {
        SimpleMailMessage m1 = new SimpleMailMessage();
        m1.setTo("a@x.com"); m1.setSubject("A"); m1.setText("a");
        SimpleMailMessage m2 = new SimpleMailMessage();
        m2.setTo("b@x.com"); m2.setSubject("B"); m2.setText("b");

        sender.send(m1, m2);

        assertEquals(2, greenMail.getReceivedMessages().length);
    }

    @Test
    void send_defaultFrom_usedWhenNotSet() throws Exception {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("to@example.com");
        msg.setSubject("From default");
        msg.setText("body");

        sender.send(msg);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("from@example.com", received[0].getFrom()[0].toString());
    }

    // -----------------------------------------------------------------------
    // MimeMessage
    // -----------------------------------------------------------------------

    @Test
    void send_mimeMessage_htmlBody_delivered() throws Exception {
        MimeMessage mime = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true);
        helper.setFrom("from@example.com");
        helper.setTo("to@example.com");
        helper.setSubject("HTML email");
        helper.setText("Plain version", "<h1>HTML version</h1>");

        sender.send(mime);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("HTML email", received[0].getSubject());
        assertInstanceOf(MimeMultipart.class, received[0].getContent());
    }

    @Test
    void send_mimeMessage_withAttachment() throws Exception {
        MimeMessage mime = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true);
        helper.setFrom("from@example.com");
        helper.setTo("to@example.com");
        helper.setSubject("With attachment");
        helper.setText("See attached");

        java.io.File tmp = java.io.File.createTempFile("lumen-test", ".txt");
        java.nio.file.Files.writeString(tmp.toPath(), "attachment content");
        helper.addAttachment("data.txt", tmp);

        sender.send(mime);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        MimeMultipart mp = (MimeMultipart) received[0].getContent();
        assertTrue(mp.getCount() > 1, "Should have body + attachment");
    }

    // -----------------------------------------------------------------------
    // MimeMessagePreparator
    // -----------------------------------------------------------------------

    @Test
    void send_preparator_populatesAndSends() throws Exception {
        sender.send(mime -> {
            MimeMessageHelper helper = new MimeMessageHelper(mime);
            helper.setFrom("from@example.com");
            helper.setTo("to@example.com");
            helper.setSubject("Preparator");
            helper.setText("via preparator");
        });

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("Preparator", received[0].getSubject());
    }

    @Test
    void send_preparatorBatch_allDelivered() {
        sender.send(
                mime -> {
                    MimeMessageHelper h = new MimeMessageHelper(mime);
                    h.setTo("a@x.com"); h.setSubject("A"); h.setText("a");
                },
                mime -> {
                    MimeMessageHelper h = new MimeMessageHelper(mime);
                    h.setTo("b@x.com"); h.setSubject("B"); h.setText("b");
                }
        );
        assertEquals(2, greenMail.getReceivedMessages().length);
    }

    // -----------------------------------------------------------------------
    // createMimeMessage
    // -----------------------------------------------------------------------

    @Test
    void createMimeMessage_returnsUsableMimeMessage() throws Exception {
        MimeMessage mime = sender.createMimeMessage();
        assertNotNull(mime);
        mime.setSubject("test");
        assertEquals("test", mime.getSubject());
    }

    // -----------------------------------------------------------------------
    // Error handling
    // -----------------------------------------------------------------------

    @Test
    void send_toInvalidHost_throwsMailSendException() {
        JavaMailSenderImpl badSender = new JavaMailSenderImpl(
                "invalid-host-that-does-not-exist.local", 25,
                "", "", "from@example.com", false, false);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("to@example.com");
        msg.setSubject("fail"); msg.setText("body");

        assertThrows(MailSendException.class, () -> badSender.send(msg));
    }

    @Test
    void send_multipleRecipients_ccAndBcc() throws Exception {
        MimeMessage mime = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime);
        helper.setFrom("from@example.com");
        helper.setTo("to@example.com");
        helper.setCc("cc@example.com");
        helper.setSubject("Multi-recipient");
        helper.setText("body");

        sender.send(mime);

        MimeMessage[] received = greenMail.getReceivedMessages();
        // GreenMail delivers one copy per recipient (TO + CC)
        assertEquals(2, received.length);
        boolean hasCc = false;
        for (MimeMessage m : received) {
            jakarta.mail.Address[] cc = m.getRecipients(Message.RecipientType.CC);
            if (cc != null && cc.length > 0) hasCc = true;
        }
        assertTrue(hasCc, "CC recipient should appear in the message headers");
    }
}