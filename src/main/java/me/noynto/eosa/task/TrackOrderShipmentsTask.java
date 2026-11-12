package me.noynto.eosa.task;

import me.noynto.eosa.application.TrackOrderShipments;

import java.util.logging.Level;
import java.util.logging.Logger;

public record TrackOrderShipmentsTask(
        TrackOrderShipments trackOrderShipments
) {
    private static final String ENABLE = "EOSA_TRACK_ORDER_SHIPMENTS_TASK";
    private static final Logger LOGGER = Logger.getLogger(TrackOrderShipmentsTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche de suivi des colis.");
        try {
            var result = trackOrderShipments.handle();
            LOGGER.log(Level.INFO, result.shipped() + " commande(s) expédiée(s), " + result.delivered() + " livrée(s), "
                    + result.emailsSent() + " e-mail(s) envoyé(s), " + result.failed() + " échec(s).");
            return result.failed() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Le suivi des colis a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
