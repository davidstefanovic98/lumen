package io.lumen.mail;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Order(2)
public class LumenMailModule implements LumenModule {

    private static final Logger logger = LoggerFactory.getLogger(LumenMailModule.class);

    @Override
    public void init(LightContainer container, String... basePackages) {
        if (container.hasLight(MailSender.class)) return; // user registered a custom sender

        Environment env  = container.getLight(Environment.class);
        String host = env != null ? env.getProperty("lumen.mail.host") : null;

        if (host == null || host.isBlank()) {
            container.registerExternalInstance(MailSender.class, new NoOpMailSender());
            logger.debug("MailSender: lumen.mail.host not configured — using NoOpMailSender");
            return;
        }

        int     port      = Integer.parseInt(env.getProperty("lumen.mail.port",           "587"));
        String  username  = env.getProperty("lumen.mail.username",   "");
        String  password  = env.getProperty("lumen.mail.password",   "");
        String  from      = env.getProperty("lumen.mail.from",        username);
        String  protocol  = env.getProperty("lumen.mail.protocol",   "smtp");
        String  encoding  = env.getProperty("lumen.mail.encoding",   "UTF-8");
        boolean auth      = Boolean.parseBoolean(env.getProperty("lumen.mail.smtp.auth",     "true"));
        boolean starttls  = Boolean.parseBoolean(env.getProperty("lumen.mail.smtp.starttls", "true"));

        JavaMailSenderImpl sender = new JavaMailSenderImpl(
                host, port, username, password, from, auth, starttls, protocol, encoding, new Properties());

        container.registerExternalInstance(MailSender.class, sender);
        container.registerExternalInstance(JavaMailSenderImpl.class, sender);

        logger.info("JavaMailSender configured → {}:{} (auth={}, starttls={})", host, port, auth, starttls);
    }
}