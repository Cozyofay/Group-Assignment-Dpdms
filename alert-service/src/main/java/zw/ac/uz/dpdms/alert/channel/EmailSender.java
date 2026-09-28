package zw.ac.uz.dpdms.alert.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends alert emails through JavaMail (SMTP settings come from .env, never from the code). */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean enabled;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${dpdms.alerts.mail.from:alerts@dpdms.local}") String from,
                       @Value("${dpdms.alerts.mail.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled && mailSender.getIfAvailable() != null;
    }

    /** Throws MailException if the mail server rejects the message; the caller logs it. */
    public void send(String to, String subject, String body) throws MailException {
        JavaMailSender sender = mailSender.getObject();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
        log.info("Alert email sent to {}", to);
    }
}
