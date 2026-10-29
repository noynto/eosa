package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

/**
 * Un lieu n'est jamais supprimé : archivé, il n'est plus proposé pour de nouvelles sessions
 * mais reste rattaché à ses sessions passées.
 */
public record ArchiveWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public WorkshopVenue handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }

        WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId)
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));

        workshopVenue.setArchived(true);
        return workshopVenueProvider.write(workshopVenue);
    }

    public record Command(
            WorkshopVenueId workshopVenueId
    ) {
    }

}
