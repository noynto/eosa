package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;

/**
 * Marque une participante absente, à partir de 30 minutes après le début (tolérance pour les retards).
 * Son adresse e-mail ne peut alors plus réserver en ligne.
 */
public record MarkWorkshopBookingNoShow(
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
        Instant noShowFrom = workshopSession.getStartsAt().plus(WorkshopBooking.NO_SHOW_GRACE_PERIOD);
        if (now.isBefore(noShowFrom)) {
            throw new RuntimeException("Une absence ne peut être marquée qu'à partir de " + WorkshopDates.time(noShowFrom) + ".");
        }
        if (workshopBooking.getState() != WorkshopBookingState.CONFIRMED
                && workshopBooking.getState() != WorkshopBookingState.ATTENDED
                && workshopBooking.getState() != WorkshopBookingState.NO_SHOW) {
            throw new RuntimeException("Seule une réservation confirmée peut être marquée absente.");
        }

        workshopBooking.setState(WorkshopBookingState.NO_SHOW);
        return workshopBookingProvider.write(workshopBooking);
    }

    public record Command(
            WorkshopSessionId workshopSessionId,
            WorkshopBookingId workshopBookingId
    ) {
    }

}
