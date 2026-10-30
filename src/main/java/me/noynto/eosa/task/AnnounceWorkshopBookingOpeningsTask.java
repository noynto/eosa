package me.noynto.eosa.task;

import me.noynto.eosa.application.AnnounceWorkshopBookingOpenings;

import java.util.logging.Level;
import java.util.logging.Logger;

public record AnnounceWorkshopBookingOpeningsTask(
        AnnounceWorkshopBookingOpenings announceWorkshopBookingOpenings
) {
    private static final String ENABLE = "EOSA_ANNOUNCE_WORKSHOP_BOOKING_OPENINGS_TASK";
    private static final Logger LOGGER = Logger.getLogger(AnnounceWorkshopBookingOpeningsTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche d'annonce des ouvertures de réservation d'atelier.");
        try {
            int notified = announceWorkshopBookingOpenings.handle();
            LOGGER.log(Level.INFO, notified + " personne(s) prévenue(s) de l'ouverture des réservations.");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "L'annonce des ouvertures de réservation d'atelier a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
