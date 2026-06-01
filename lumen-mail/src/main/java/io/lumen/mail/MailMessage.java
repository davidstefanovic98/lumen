package io.lumen.mail;

import java.util.Date;

public interface MailMessage {

    void setFrom(String from);

    void setReplyTo(String replyTo);

    void setTo(String to);

    void setTo(String... to);

    void setCc(String cc);

    void setCc(String... cc);

    void setBcc(String bcc);

    void setBcc(String... bcc);

    void setSentDate(Date sentDate);

    void setSubject(String subject);

    void setText(String text);
}
