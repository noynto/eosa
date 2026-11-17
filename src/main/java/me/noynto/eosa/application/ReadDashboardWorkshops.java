package me.noynto.eosa.application;

import me.noynto.eosa.service.WorkshopPlaces;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Les prochaines sessions d'atelier à venir et non annulées, par date croissante, avec leurs places occupées. */
public record ReadDashboardWorkshops(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        Clock clock
) {

    static final int COUNT = 3;

    public Result handle() {
        Instant now = clock.instant();
        return new Result(workshopSessionProvider.readIds()
                .map(id -> workshopSessionProvider.read(id).orElseThrow())
                .filter(session -> session.getState() == WorkshopSessionState.SCHEDULED)
                .filter(session -> session.getEndsAt().isAfter(now))
                .limit(COUNT)
                .map(session -> overviewOf(session, now))
                .toList());
    }

    private Overview overviewOf(WorkshopSession session, Instant now) {
        WorkshopVenue venue = workshopVenueProvider.read(session.getVenueId()).orElseThrow();
        var bookings = workshopBookingProvider
                .readIds(session.getId(), WorkshopPlaces.OCCUPYING_STATES, null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        int occupiedPlaces = WorkshopPlaces.occupied(bookings, now);
        int waitlistCount = (int) workshopWaitlistEntryProvider.readIds(session.getId(), null, null, true).count();
        return new Overview(session, venue, occupiedPlaces, waitlistCount);
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
