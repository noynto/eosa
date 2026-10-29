package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopVenueId;

import java.util.Optional;
import java.util.stream.Stream;

public interface WorkshopVenueProvider {

    /**
     * Identifiants des lieux, les lieux actifs d'abord, puis par nom.
     */
    Stream<WorkshopVenueId> readIds();

    Optional<WorkshopVenue> read(WorkshopVenueId workshopVenueId);

    WorkshopVenue write(WorkshopVenue workshopVenue);

}
