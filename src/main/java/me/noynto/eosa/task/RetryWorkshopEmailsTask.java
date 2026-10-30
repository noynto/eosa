package me.noynto.eosa.task;

import me.noynto.eosa.application.RetryWorkshopEmails;

import java.util.logging.Level;
import java.util.logging.Logger;

public record RetryWorkshopEmailsTask(
        RetryWorkshopEmails retryWorkshopEmails
) {
    private static final String ENABLE = "EOSA_RETRY_WORKSHOP_EMAILS_TASK";
    private static final Logger LOGGER = Logger.getLogger(RetryWorkshopEmailsTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche de renvoi des e-mails d'atelier.");
        try {
            var result = retryWorkshopEmails.handle();
            LOGGER.log(Level.INFO, result.sent() + " e-mail(s) d'atelier renvoyé(s), " + result.failed() + " en échec.");
            // Un e-mail encore en échec sera retenté au prochain passage : la tâche échoue pour le signaler.
            return result.failed() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Le renvoi des e-mails d'atelier a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
