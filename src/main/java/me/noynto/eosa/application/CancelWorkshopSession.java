package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annulation d'une session : ses réservations encore actives sont annulées et chaque inscrite est prévenue.
 */
public record CancelWorkshopSession(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopBookingProvider workshopBookingProvider,
        SendWorkshopBookingCancellation sendWorkshopBookingCancellation,
        Clock clock
) {

    private static final Logger LOGGER = Logger.getLogger(CancelWorkshopSession.class.getName());

    public WorkshopSession handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }

        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        if (workshopSession.getState() == WorkshopSessionState.CANCELLED) {
            throw new RuntimeException("La session est déjà annulée.");
        }
        Instant now = clock.instant();
        if (!workshopSession.getStartsAt().isAfter(now)) {
            throw new RuntimeException("Une session commencée ne peut plus être annulée.");
        }

        workshopSession.setState(WorkshopSessionState.CANCELLED);
        workshopSession.setCancelledAt(now);
        workshopSessionProvider.write(workshopSession);

        var activeBookingIds = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED), null, null)
                .toList();
        for (var bookingId : activeBookingIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(bookingId).orElseThrow();
            if (!workshopBooking.isActive(now)) {
                continue;
            }
            workshopBooking.setState(WorkshopBookingState.CANCELLED);
            workshopBooking.setCancelledAt(now);
            workshopBookingProvider.write(workshopBooking);
            // Un e-mail qui ne part pas sera renvoyé plus tard : il ne doit pas bloquer les autres.
            try {
                sendWorkshopBookingCancellation.handle(new SendWorkshopBookingCancellation.Command(bookingId));
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail d'annulation de la réservation " + bookingId.value()
                        + " n'est pas parti, il sera renvoyé plus tard.", e);
            }
        }
        return workshopSession;
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
