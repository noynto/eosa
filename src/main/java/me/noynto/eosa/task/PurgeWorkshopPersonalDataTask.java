package me.noynto.eosa.task;

import me.noynto.eosa.application.PurgeWorkshopPersonalData;

import java.util.logging.Level;
import java.util.logging.Logger;

public record PurgeWorkshopPersonalDataTask(
        PurgeWorkshopPersonalData purgeWorkshopPersonalData
) {
    private static final String ENABLE = "EOSA_PURGE_WORKSHOP_PERSONAL_DATA_TASK";
    private static final Logger LOGGER = Logger.getLogger(PurgeWorkshopPersonalDataTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche de suppression des données personnelles des ateliers.");
        try {
            var result = purgeWorkshopPersonalData.handle();
            LOGGER.log(Level.INFO, result.deletedBookings() + " réservation(s) et " + result.deletedWaitlistEntries()
                    + " inscription(s) en liste d'attente supprimée(s).");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "La suppression des données personnelles des ateliers a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
