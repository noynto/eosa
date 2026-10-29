package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.util.List;

public record ReadWorkshopSessions(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider
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
        int occupiedPlaces = workshopBookingProvider.countOccupiedPlaces(workshopSession.getId());
        return new Overview(workshopSession, workshopVenue, occupiedPlaces);
    }

    public record Overview(
            WorkshopSession session,
            WorkshopVenue venue,
            int occupiedPlaces
    ) {
    }

}
