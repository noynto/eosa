package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

/**
 * Réservation retrouvée par le jeton de ses liens (confirmation, annulation), avec sa session et son lieu.
 */
public record ReadWorkshopBooking(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider
) {

    public Overview handle(Command command) {
        if (command.token == null || command.token.isBlank()) {
            throw new RuntimeException("Le lien de réservation est incomplet.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.readIds(null, null, null, command.token())
                .findFirst()
                .flatMap(workshopBookingProvider::read)
                .orElseThrow(() -> new RuntimeException("Ce lien de réservation n'est pas valide."));
        WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        return new Overview(workshopBooking, workshopSession, workshopVenue);
    }

    public record Command(
            String token
    ) {
    }

    public record Overview(
            WorkshopBooking booking,
            WorkshopSession session,
            WorkshopVenue venue
    ) {
    }

}
