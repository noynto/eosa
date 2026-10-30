package me.noynto.eosa.task;

import me.noynto.eosa.application.ExpirePendingWorkshopBookings;

import java.util.logging.Level;
import java.util.logging.Logger;

public record ExpirePendingWorkshopBookingsTask(
        ExpirePendingWorkshopBookings expirePendingWorkshopBookings
) {
    private static final String ENABLE = "EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK";
    private static final Logger LOGGER = Logger.getLogger(ExpirePendingWorkshopBookingsTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche d'expiration des réservations d'atelier non confirmées.");
        try {
            int expired = expirePendingWorkshopBookings.handle();
            LOGGER.log(Level.INFO, expired + " réservation(s) d'atelier non confirmée(s) expirée(s).");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "L'expiration des réservations d'atelier non confirmées a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
