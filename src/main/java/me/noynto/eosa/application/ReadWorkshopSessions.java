package me.noynto.eosa.application;

import me.noynto.eosa.service.WorkshopPlaces;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public record ReadWorkshopSessions(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        Clock clock
) {

    /**
     * Toutes les sessions, par date de début croissante, avec leur lieu et leurs places occupées.
     */
    public Result handle() {
        return new Result(workshopSessionProvider.readIds()
                .map(id -> overviewOf(workshopSessionProvider.read(id).orElseThrow()))
                .toList());
    }

    Overview overviewOf(WorkshopSession workshopSession) {
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        Instant now = clock.instant();
        var bookings = workshopBookingProvider
                .readIds(workshopSession.getId(), WorkshopPlaces.OCCUPYING_STATES, null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        int occupiedPlaces = WorkshopPlaces.occupied(bookings, now);
        int waitlistCount = (int) workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).count();
        return new Overview(workshopSession, workshopVenue, occupiedPlaces, waitlistCount);
    }

    public record Result(
            List<Overview> overviews
    ) {
    }

    public record Overview(
            WorkshopSession session,
            WorkshopVenue venue,
            int occupiedPlaces,
            int waitlistCount
    ) {
    }

}
