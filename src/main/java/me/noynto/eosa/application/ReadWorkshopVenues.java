package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.util.List;

public record ReadWorkshopVenues(
        WorkshopVenueProvider workshopVenueProvider
) {

    public List<WorkshopVenue> handle() {
        return workshopVenueProvider.readIds()
                .map(id -> workshopVenueProvider.read(id).orElseThrow())
                .toList();
    }

}
