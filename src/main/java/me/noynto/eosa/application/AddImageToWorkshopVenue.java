package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

public record AddImageToWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider,
        ImageProvider imageProvider
) {

    public WorkshopVenue handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu sur lequel ajouter une photo est nécessaire.");
        }
        if (command.image == null) {
            throw new RuntimeException("Une photo est nécessaire.");
        }

        WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId())
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));

        Image uploaded = imageProvider.upload(command.image());
        workshopVenue.setImageId(uploaded.getId());

        return workshopVenueProvider.write(workshopVenue);
    }

    public record Command(
            WorkshopVenueId workshopVenueId,
            Image image
    ) {
    }

}
