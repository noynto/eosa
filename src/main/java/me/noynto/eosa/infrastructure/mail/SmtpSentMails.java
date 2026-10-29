package me.noynto.eosa.infrastructure.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import me.noynto.eosa.infrastructure.mail.config.SmtpProperties;
import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

public class SmtpSentMails implements MailProvider {

    private static final String SENDER_NAME = "EOSA";
    private static final String TIMEOUT_MILLISECONDS = "10000";

    private final SmtpProperties properties;
    private final MailTemplates mailTemplates;
    private final Session session;

    public SmtpSentMails(SmtpProperties properties, MailTemplates mailTemplates) {
        this.properties = properties;
        this.mailTemplates = mailTemplates;
        this.session = Session.getInstance(sessionProperties(properties), authenticator(properties));
    }

    @Override
    public void send(Mail mail) {
        MailTemplates.Rendered rendered = mailTemplates.render(mail.template(), mail.model());
        try {
            MimeMessage message = new MimeMessage(session);
            InternetAddress sender = new InternetAddress(properties.from(), SENDER_NAME, StandardCharsets.UTF_8.name());
            message.setFrom(sender);
            message.setReplyTo(new InternetAddress[]{sender});
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.to(), true));
            message.setSubject(mail.subject(), StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());

            MimeBodyPart text = new MimeBodyPart();
            text.setText(rendered.text(), StandardCharsets.UTF_8.name());
            MimeBodyPart html = new MimeBodyPart();
            html.setText(rendered.html(), StandardCharsets.UTF_8.name(), "html");
            // Du moins riche au plus riche : le client affiche la dernière version qu'il sait lire.
            message.setContent(new MimeMultipart("alternative", text, html));

            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Impossible d'envoyer l'e-mail « " + mail.subject() + " ».", e);
        }
    }

    private static Properties sessionProperties(SmtpProperties properties) {
        Properties sessionProperties = new Properties();
        sessionProperties.put("mail.smtp.host", properties.host());
        sessionProperties.put("mail.smtp.port", String.valueOf(properties.port()));
        sessionProperties.put("mail.smtp.auth", String.valueOf(properties.username() != null));
        sessionProperties.put("mail.smtp.connectiontimeout", TIMEOUT_MILLISECONDS);
        sessionProperties.put("mail.smtp.timeout", TIMEOUT_MILLISECONDS);
        sessionProperties.put("mail.smtp.writetimeout", TIMEOUT_MILLISECONDS);
        switch (properties.security()) {
            case SSL -> {
                sessionProperties.put("mail.smtp.ssl.enable", "true");
                sessionProperties.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            case STARTTLS -> {
                sessionProperties.put("mail.smtp.starttls.enable", "true");
                sessionProperties.put("mail.smtp.starttls.required", "true");
                sessionProperties.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            case NONE -> {
            }
        }
        return sessionProperties;
    }

    private static Authenticator authenticator(SmtpProperties properties) {
        if (properties.username() == null) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(properties.username(), properties.password());
            }
        };
    }

}
