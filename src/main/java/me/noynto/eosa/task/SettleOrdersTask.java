package me.noynto.eosa.task;

import me.noynto.eosa.application.SettleOrders;

import java.util.logging.Level;
import java.util.logging.Logger;

public record SettleOrdersTask(
        SettleOrders settleOrders
) {
    private static final String ENABLE = "EOSA_SETTLE_ORDERS_TASK";
    private static final Logger LOGGER = Logger.getLogger(SettleOrdersTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche de rattrapage des commandes payées.");
        try {
            var result = settleOrders.handle();
            LOGGER.log(Level.INFO, result.settled() + " commande(s) rattrapée(s), " + result.emailsSent() + " e-mail(s) envoyé(s), "
                    + result.failed() + " échec(s).");
            return result.failed() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Le rattrapage des commandes payées a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
