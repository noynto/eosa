package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Envoie un e-mail de test, pour vérifier la configuration SMTP et le rendu du gabarit commun.
 */
public record SendTestEmail(
        MailProvider mailProvider
) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public void handle(Command command) {
        if (command.to == null || !EMAIL.matcher(command.to.strip()).matches()) {
            throw new RuntimeException("Une adresse e-mail valide est nécessaire.");
        }
        mailProvider.send(new Mail(
                command.to().strip(),
                "Test d'envoi — EOSA",
                "test",
                Map.of("preheader", "Si vous lisez ceci, l'envoi d'e-mails fonctionne.")
        ));
    }

    public record Command(
            String to
    ) {
    }

}
