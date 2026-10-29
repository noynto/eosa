package me.noynto.eosa.task;

import me.noynto.eosa.application.SendTestEmail;

import java.util.logging.Level;
import java.util.logging.Logger;

public record SendTestEmailTask(
        SendTestEmail sendTestEmail
) {
    private static final String TO = "EOSA_SEND_TEST_EMAIL_TO";
    private static final Logger LOGGER = Logger.getLogger(SendTestEmailTask.class.getName());

    public boolean task() {
        String to = System.getenv(TO);
        LOGGER.log(Level.INFO, "Démarrage de la tâche d'envoi d'un e-mail de test à " + to + ".");
        try {
            sendTestEmail.handle(new SendTestEmail.Command(to));
            LOGGER.log(Level.INFO, "L'e-mail de test est envoyé à " + to + ".");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "L'e-mail de test n'est pas envoyé.", e);
            return false;
        }
    }

    public static boolean activate() {
        String to = System.getenv(TO);
        return to != null && !to.isBlank();
    }
}
