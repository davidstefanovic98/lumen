package io.lumen.mail;

/**
 * Central interface for sending email.
 * Inject this interface to send mail; the implementation is determined by
 * which provider is on the classpath (JavaMail by default via lumen-boot-starter-mail).
 * Register a custom implementation as a @Light bean to override the default.
 */
public interface MailSender {

    /**
     * Send the given simple mail message.
     * @param simpleMessage the message to send
     * @throws MailException in case of any failure
     */
    // TODO: divide MailException into more exception, too broad
    default void send(SimpleMailMessage simpleMessage) throws MailException {
        send(new SimpleMailMessage[] {simpleMessage});
    }

    /**
     * Send the given array of simple mail messages in batch.
     * @param simpleMessages the messages to send
     * @throws MailException in case of any failure
     */
    void send(SimpleMailMessage... simpleMessages) throws MailException;
}