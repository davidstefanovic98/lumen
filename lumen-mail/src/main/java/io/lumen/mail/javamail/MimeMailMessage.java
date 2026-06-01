package io.lumen.mail.javamail;

import io.lumen.mail.MailMessage;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;

/**
 * Adapter that wraps a {@link MimeMessage} as a {@link MailMessage}, delegating
 * all setters through a {@link MimeMessageHelper}.
 * <p>
 * Useful when you want to pass a MimeMessage to code that accepts a MailMessage.
 */
public class MimeMailMessage implements MailMessage {

    private final MimeMessageHelper helper;

    public MimeMailMessage(MimeMessage mimeMessage) {
        this.helper = new MimeMessageHelper(mimeMessage);
    }

    public MimeMailMessage(MimeMessageHelper helper) {
        this.helper = helper;
    }

    public MimeMessage getMimeMessage() {
        return helper.getMimeMessage();
    }

    public MimeMessageHelper getMimeMessageHelper() {
        return helper;
    }

    @Override
    public void setFrom(String from) {
        helper.setFrom(from);
    }

    @Override
    public void setReplyTo(String replyTo) {
        helper.setReplyTo(replyTo);
    }

    @Override
    public void setTo(String to) {
        helper.setTo(to);
    }

    @Override
    public void setTo(String... to) {
        helper.setTo(to);
    }

    @Override
    public void setCc(String cc) {
        helper.setCc(cc);
    }

    @Override
    public void setCc(String... cc) {
        helper.setCc(cc);
    }

    @Override
    public void setBcc(String bcc) {
        helper.setBcc(bcc);
    }

    @Override
    public void setBcc(String... bcc) {
        helper.setBcc(bcc);
    }

    @Override
    public void setSentDate(Date sentDate) {
        helper.setSentDate(sentDate);
    }

    @Override
    public void setSubject(String subject) {
        helper.setSubject(subject);
    }

    @Override
    public void setText(String text) {
        helper.setText(text);
    }
}