package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record ReadWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public Result handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }
        return new Result(workshopVenueProvider.read(command.workshopVenueId())
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas.")));
    }

    public record Result(
            WorkshopVenue venue
    ) {
    }

    public record Command(
            WorkshopVenueId workshopVenueId
    ) {
    }

}
