package me.noynto.eosa.application;

import me.noynto.eosa.service.WorkshopPlaces;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.time.Clock;
import java.time.Instant;

/**
 * Une session avec son lieu, ses places occupées et le nombre de personnes en liste d'attente.
 */
public record ReadWorkshopSession(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId())
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        Instant now = clock.instant();
        var bookings = workshopBookingProvider
                .readIds(workshopSession.getId(), WorkshopPlaces.OCCUPYING_STATES, null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        int waitlistCount = (int) workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).count();
        return new Result(workshopSession, workshopVenue, WorkshopPlaces.occupied(bookings, now), waitlistCount);
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

    public record Result(
            WorkshopSession session,
            WorkshopVenue venue,
            int occupiedPlaces,
            int waitlistCount
    ) {
    }

}
