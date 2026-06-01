package io.lumen.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoOpMailSenderTest {

    private final NoOpMailSender sender = new NoOpMailSender();

    @Test
    void send_doesNotThrow() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("user@example.com");
        msg.setSubject("Test");
        msg.setText("Body");
        assertDoesNotThrow(() -> sender.send(msg));
    }

    @Test
    void send_batch_doesNotThrow() {
        SimpleMailMessage m1 = new SimpleMailMessage();
        m1.setTo("a@x.com"); m1.setSubject("A"); m1.setText("body");
        SimpleMailMessage m2 = new SimpleMailMessage();
        m2.setTo("b@x.com"); m2.setSubject("B"); m2.setText("body");
        assertDoesNotThrow(() -> sender.send(m1, m2));
    }

    @Test
    void defaultSend_delegatesToBatch() {
        // The default send(SimpleMailMessage) must call send(SimpleMailMessage...)
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("user@example.com");
        msg.setSubject("S"); msg.setText("T");
        assertDoesNotThrow(() -> sender.send(msg));
    }
}