package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record CreateWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public static final int DEFAULT_CAPACITY = 8;
    public static final int DEFAULT_DURATION_MINUTES = 120;

    public WorkshopVenue handle(Command command) {
        if (command.name == null || command.name.isBlank()) {
            throw new RuntimeException("Le nom du lieu est requis.");
        }
        if (command.address == null || command.address.isBlank()) {
            throw new RuntimeException("L'adresse du lieu est requise.");
        }
        if (command.postalCode == null || command.postalCode.isBlank()) {
            throw new RuntimeException("Le code postal du lieu est requis.");
        }
        if (command.city == null || command.city.isBlank()) {
            throw new RuntimeException("La ville du lieu est requise.");
        }

        WorkshopVenue workshopVenue = new WorkshopVenue();
        workshopVenue.setName(command.name().strip());
        workshopVenue.setAddress(command.address().strip());
        workshopVenue.setPostalCode(command.postalCode().strip());
        workshopVenue.setCity(command.city().strip());
        workshopVenue.setDefaultCapacity(DEFAULT_CAPACITY);
        workshopVenue.setDefaultDurationMinutes(DEFAULT_DURATION_MINUTES);
        return workshopVenueProvider.write(workshopVenue);
    }

    public record Command(
            String name,
            String address,
            String postalCode,
            String city
    ) {
    }

}
