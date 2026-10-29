package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record UpdateDefaultsOfWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public static final int MAX_CAPACITY = 50;
    public static final int MIN_DURATION_MINUTES = 15;
    public static final int MAX_DURATION_MINUTES = 480;

    public WorkshopVenue handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }
        if (command.defaultCapacity == null || command.defaultCapacity < 1 || command.defaultCapacity > MAX_CAPACITY) {
            throw new RuntimeException("La capacité par défaut doit être comprise entre 1 et " + MAX_CAPACITY + " places.");
        }
        if (command.defaultDurationMinutes == null
                || command.defaultDurationMinutes < MIN_DURATION_MINUTES
                || command.defaultDurationMinutes > MAX_DURATION_MINUTES) {
            throw new RuntimeException("La durée par défaut doit être comprise entre "
                    + MIN_DURATION_MINUTES + " et " + MAX_DURATION_MINUTES + " minutes.");
        }

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
