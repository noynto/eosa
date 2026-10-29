package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.net.URI;
import java.net.URISyntaxException;

public record UpdateWorkshopVenue(
        WorkshopVenueProvider workshopVenueProvider
) {

    public WorkshopVenue handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("L'identifiant du lieu est nécessaire.");
        }
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
        String linkUrl = blankToNull(command.linkUrl());
        if (linkUrl != null && !isWebUrl(linkUrl)) {
            throw new RuntimeException("Le lien doit être une adresse web commençant par https:// ou http://.");
        }

        WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId)
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));

        workshopVenue.setName(command.name().strip());
        workshopVenue.setDescription(blankToNull(command.description()));
        workshopVenue.setAddress(command.address().strip());
        workshopVenue.setPostalCode(command.postalCode().strip());
        workshopVenue.setCity(command.city().strip());
        workshopVenue.setLinkUrl(linkUrl);
        return workshopVenueProvider.write(workshopVenue);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    // Le lien est rendu dans un href : seuls http et https sont acceptés (pas de javascript:…).
    private static boolean isWebUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public record Command(
            WorkshopVenueId workshopVenueId,
            String name,
            String description,
            String address,
            String postalCode,
            String city,
            String linkUrl
    ) {
    }

}
