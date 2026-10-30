package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

import java.time.Clock;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annulation d'une réservation par l'administration, sans contrainte de délai (motif valable reçu sur Instagram…).
 */
public record CancelWorkshopBookingByAdmin(
        WorkshopBookingProvider workshopBookingProvider,
        SendWorkshopBookingCancellation sendWorkshopBookingCancellation,
        NotifyWorkshopWaitlist notifyWorkshopWaitlist,
        Clock clock
) {

    private static final Logger LOGGER = Logger.getLogger(CancelWorkshopBookingByAdmin.class.getName());

    public WorkshopBooking handle(Command command) {
        if (command.workshopSessionId == null || command.workshopBookingId == null || command.workshopBookingId.value() == null) {
            throw new RuntimeException("La session et la réservation sont nécessaires.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.read(command.workshopBookingId)
                .filter(booking -> booking.getSessionId().equals(command.workshopSessionId()))
                .orElseThrow(() -> new RuntimeException("Cette réservation n'existe pas sur cette session."));
        Instant now = clock.instant();
        if (!workshopBooking.isActive(now)) {
            throw new RuntimeException("Cette réservation n'est plus active.");
        }

        workshopBooking.setState(WorkshopBookingState.CANCELLED);
        workshopBooking.setCancelledAt(now);
        workshopBookingProvider.write(workshopBooking);
        try {
            sendWorkshopBookingCancellation.handle(new SendWorkshopBookingCancellation.Command(workshopBooking.getId()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail d'annulation de la réservation " + workshopBooking.getId().value()
                    + " n'est pas parti, il sera renvoyé plus tard.", e);
        }
        try {
            notifyWorkshopWaitlist.handle(new NotifyWorkshopWaitlist.Command(workshopBooking.getSessionId()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "La liste d'attente de la session " + workshopBooking.getSessionId().value()
                    + " n'a pas pu être prévenue.", e);
        }
        return workshopBooking;
    }

    public record Command(
            WorkshopSessionId workshopSessionId,
            WorkshopBookingId workshopBookingId
    ) {
    }

}
