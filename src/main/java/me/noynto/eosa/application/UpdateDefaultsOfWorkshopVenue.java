package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record UpdateDefaultsOfWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public Result handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }
        WorkshopSession.checkCapacity(command.defaultCapacity());
        WorkshopSession.checkDuration(command.defaultDurationMinutes());

        WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId)
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));

        workshopVenue.setDefaultCapacity(command.defaultCapacity());
        workshopVenue.setDefaultDurationMinutes(command.defaultDurationMinutes());
        return new Result(workshopVenueProvider.write(workshopVenue));
    }

    public record Result(
            WorkshopVenue venue
    ) {
    }

    public record Command(
            WorkshopVenueId workshopVenueId,
            Integer defaultCapacity,
            Integer defaultDurationMinutes
    ) {
    }

}
