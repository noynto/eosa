package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;

/**
 * Marque une participante présente, dès le début de la séance. Corrige aussi une absence marquée par
 * erreur, ce qui lui rend la réservation en ligne.
 */
public record MarkWorkshopBookingAttended(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        Clock clock
) {

    public WorkshopBooking handle(Command command) {
        if (command.workshopSessionId == null || command.workshopBookingId == null || command.workshopBookingId.value() == null) {
            throw new RuntimeException("La session et la réservation sont nécessaires.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.read(command.workshopBookingId)
                .filter(booking -> booking.getSessionId().equals(command.workshopSessionId()))
                .orElseThrow(() -> new RuntimeException("Cette réservation n'existe pas sur cette session."));
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        Instant now = clock.instant();
        if (workshopSession.getState() == WorkshopSessionState.CANCELLED) {
            throw new RuntimeException("Cette session a été annulée.");
        }
        if (now.isBefore(workshopSession.getStartsAt())) {
            throw new RuntimeException("La séance n'a pas encore commencé.");
        }
        if (workshopBooking.getState() != WorkshopBookingState.CONFIRMED
                && workshopBooking.getState() != WorkshopBookingState.NO_SHOW
                && workshopBooking.getState() != WorkshopBookingState.ATTENDED) {
            throw new RuntimeException("Seule une réservation confirmée peut être marquée présente.");
        }

        workshopBooking.setState(WorkshopBookingState.ATTENDED);
        return workshopBookingProvider.write(workshopBooking);
    }

    public record Command(
            WorkshopSessionId workshopSessionId,
            WorkshopBookingId workshopBookingId
    ) {
    }

}
