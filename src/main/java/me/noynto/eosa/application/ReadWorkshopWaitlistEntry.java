package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

/**
 * Inscription en liste d'attente retrouvée par le jeton de son lien de désinscription.
 */
public record ReadWorkshopWaitlistEntry(
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider
) {

    public Overview handle(Command command) {
        if (command.token == null || command.token.isBlank()) {
            throw new RuntimeException("Le lien est incomplet.");
        }
        WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.readIds(null, null, command.token(), null)
                .findFirst()
                .flatMap(workshopWaitlistEntryProvider::read)
                .orElseThrow(() -> new RuntimeException("Ce lien n'est pas valide."));
        WorkshopSession workshopSession = workshopSessionProvider.read(entry.getSessionId()).orElseThrow();
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        return new Overview(entry, workshopSession, workshopVenue);
    }

    public record Command(
            String token
    ) {
    }

    public record Overview(
            WorkshopWaitlistEntry entry,
            WorkshopSession session,
            WorkshopVenue venue
    ) {
    }

}
