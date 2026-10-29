package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopSessionId;

import java.util.Optional;
import java.util.stream.Stream;

public interface WorkshopSessionProvider {

    /**
     * Identifiants de toutes les sessions, par date de début croissante.
     */
    Stream<WorkshopSessionId> readIds();

    Optional<WorkshopSession> read(WorkshopSessionId workshopSessionId);

    WorkshopSession write(WorkshopSession workshopSession);

}
