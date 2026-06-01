package io.lumen.mail;

import java.util.Date;

public class SimpleMailMessage implements MailMessage {

    private String from;
    private String replyTo;
    private String[] to;
    private String[] cc;
    private String[] bcc;
    private Date sentDate;
    private String subject;
    private String text;


    @Override
    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    @Override
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public void setTo(String to) {
        this.to = new String[] { to };
    }

    public String[] getTo() {
        return to;
    }

    @Override
    public void setTo(String... to) {
        this.to = to;
    }

    @Override
    public void setCc(String cc) {
        this.cc = new String[] { cc };
    }

    @Override
    public void setCc(String... cc) {
        this.cc = cc;
    }

    public String[] getCc() {
        return cc;
    }

    @Override
    public void setBcc(String bcc) {
        this.bcc = new String[] { bcc };
    }

    @Override
    public void setBcc(String... bcc) {
        this.bcc = bcc;
    }

    public String[] getBcc() {
        return bcc;
    }

    @Override
    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public Date getSentDate() {
        return sentDate;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}