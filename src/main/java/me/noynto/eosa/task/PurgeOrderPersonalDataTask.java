package me.noynto.eosa.task;

import me.noynto.eosa.application.PurgeOrderPersonalData;

import java.util.logging.Level;
import java.util.logging.Logger;

public record PurgeOrderPersonalDataTask(
        PurgeOrderPersonalData purgeOrderPersonalData
) {
    private static final String ENABLE = "EOSA_PURGE_ORDER_PERSONAL_DATA_TASK";
    private static final Logger LOGGER = Logger.getLogger(PurgeOrderPersonalDataTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche d'effacement des données personnelles des commandes.");
        try {
            var result = purgeOrderPersonalData.handle();
            LOGGER.log(Level.INFO, result.purged() + " commande(s) effacée(s), " + result.failed() + " échec(s).");
            return result.failed() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "L'effacement des données personnelles des commandes a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
