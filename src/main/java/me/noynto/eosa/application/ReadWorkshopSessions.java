package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

public record ReadWorkshopSessions(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        Clock clock
) {

    /**
     * Toutes les sessions, par date de début croissante, avec leur lieu et leurs places occupées.
     */
    public List<Overview> handle() {
        return workshopSessionProvider.readIds()
                .map(id -> overviewOf(workshopSessionProvider.read(id).orElseThrow()))
                .toList();
    }

    Overview overviewOf(WorkshopSession workshopSession) {
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        Instant now = clock.instant();
        int occupiedPlaces = (int) workshopBookingProvider.readIds(
                        workshopSession.getId(),
                        EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
                                WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW),
                        null,
                        null
                )
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .filter(booking -> booking.occupiesAPlace(now))
                .count();
        return new Overview(workshopSession, workshopVenue, occupiedPlaces);
    }

    public record Overview(
            WorkshopSession session,
            WorkshopVenue venue,
            int occupiedPlaces
    ) {
    }

}
