package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record ReadWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public WorkshopVenue handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }
        return workshopVenueProvider.read(command.workshopVenueId())
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));
    }

    public record Command(
            WorkshopVenueId workshopVenueId
    ) {
    }

}
