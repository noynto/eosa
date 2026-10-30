package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopWaitlistEntryId;

import java.util.Optional;
import java.util.stream.Stream;

public interface WorkshopWaitlistEntryProvider {

    /**
     * Identifiants des inscriptions, de la plus ancienne à la plus récente. Chaque filtre est ignoré
     * s'il est null ; l'e-mail est comparé sans tenir compte de la casse ; {@code waiting} à true ne
     * garde que les inscriptions encore en attente.
     */
    Stream<WorkshopWaitlistEntryId> readIds(WorkshopSessionId sessionId, String email, String token, Boolean waiting);

    Optional<WorkshopWaitlistEntry> read(WorkshopWaitlistEntryId workshopWaitlistEntryId);

    WorkshopWaitlistEntry write(WorkshopWaitlistEntry workshopWaitlistEntry);

}
