package me.noynto.eosa.task;

import me.noynto.eosa.application.SendWorkshopReminders;

import java.util.logging.Level;
import java.util.logging.Logger;

public record SendWorkshopRemindersTask(
        SendWorkshopReminders sendWorkshopReminders
) {
    private static final String ENABLE = "EOSA_SEND_WORKSHOP_REMINDERS_TASK";
    private static final Logger LOGGER = Logger.getLogger(SendWorkshopRemindersTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche d'envoi des rappels d'atelier.");
        try {
            var result = sendWorkshopReminders.handle();
            LOGGER.log(Level.INFO, result.sent() + " rappel(s) d'atelier envoyé(s), " + result.failed() + " en échec.");
            // Un rappel en échec sera retenté au prochain passage : la tâche échoue pour le signaler.
            return result.failed() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "L'envoi des rappels d'atelier a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
