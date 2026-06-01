package io.lumen.mail;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMailMessageTest {

    @Test
    void setTo_singleAddress_storedAsArray() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("user@example.com");
        assertArrayEquals(new String[]{"user@example.com"}, msg.getTo());
    }

    @Test
    void setTo_varargs_storedDirectly() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("a@x.com", "b@x.com");
        assertArrayEquals(new String[]{"a@x.com", "b@x.com"}, msg.getTo());
    }

    @Test
    void setCc_singleAddress_storedAsArray() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setCc("cc@example.com");
        assertArrayEquals(new String[]{"cc@example.com"}, msg.getCc());
    }

    @Test
    void setBcc_singleAddress_storedAsArray() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setBcc("bcc@example.com");
        assertArrayEquals(new String[]{"bcc@example.com"}, msg.getBcc());
    }

    @Test
    void allFields_roundtrip() {
        Date sent = new Date();
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("from@example.com");
        msg.setReplyTo("reply@example.com");
        msg.setTo("to@example.com");
        msg.setSubject("Hello");
        msg.setText("Body text");
        msg.setSentDate(sent);

        assertEquals("from@example.com", msg.getFrom());
        assertEquals("reply@example.com", msg.getReplyTo());
        assertEquals("to@example.com", msg.getTo()[0]);
        assertEquals("Hello", msg.getSubject());
        assertEquals("Body text", msg.getText());
        assertEquals(sent, msg.getSentDate());
    }
}