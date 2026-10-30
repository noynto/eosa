package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record UpdateDefaultsOfWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public static final int MAX_CAPACITY = WorkshopSessionRules.MAX_CAPACITY;
    public static final int MIN_DURATION_MINUTES = WorkshopSessionRules.MIN_DURATION_MINUTES;
    public static final int MAX_DURATION_MINUTES = WorkshopSessionRules.MAX_DURATION_MINUTES;

    public WorkshopVenue handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }
        WorkshopSessionRules.checkCapacity(command.defaultCapacity());
        WorkshopSessionRules.checkDuration(command.defaultDurationMinutes());

        WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId)
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));

        workshopVenue.setDefaultCapacity(command.defaultCapacity());
        workshopVenue.setDefaultDurationMinutes(command.defaultDurationMinutes());
        return workshopVenueProvider.write(workshopVenue);
    }

    public record Command(
            WorkshopVenueId workshopVenueId,
            Integer defaultCapacity,
            Integer defaultDurationMinutes
    ) {
    }

}
