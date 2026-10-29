package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

public record CreateWorkshopSession(
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopSessionProvider workshopSessionProvider,
        Clock clock
) {

    public WorkshopSession handle(Command command) {
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("Le lieu de la session est requis.");
        }
        if (command.startsAt == null) {
            throw new RuntimeException("La date et l'heure de début sont requises.");
        }

        WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId)
                .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));
        if (workshopVenue.isArchived()) {
            throw new RuntimeException("Le lieu « " + workshopVenue.getName() + " » est archivé.");
        }

        Instant startsAt = command.startsAt().atZone(WorkshopSession.ZONE).toInstant();
        if (!startsAt.isAfter(clock.instant())) {
            throw new RuntimeException("La session doit commencer dans le futur.");
        }
        int capacity = command.capacity() != null ? command.capacity() : workshopVenue.getDefaultCapacity();
        int durationMinutes = command.durationMinutes() != null ? command.durationMinutes() : workshopVenue.getDefaultDurationMinutes();
        WorkshopSessionRules.checkCapacity(capacity);
        WorkshopSessionRules.checkDuration(durationMinutes);

        WorkshopSession workshopSession = new WorkshopSession();
        workshopSession.setVenueId(workshopVenue.getId());
        workshopSession.setStartsAt(startsAt);
        workshopSession.setDurationMinutes(durationMinutes);
        workshopSession.setCapacity(capacity);
        workshopSession.setState(WorkshopSessionState.SCHEDULED);
        return workshopSessionProvider.write(workshopSession);
    }

    /**
     * @param startsAt        date et heure de début, en heure de Paris
     * @param durationMinutes durée ; celle par défaut du lieu si absente
     * @param capacity        nombre de places ; celui par défaut du lieu si absent
     */
    public record Command(
            WorkshopVenueId workshopVenueId,
            LocalDateTime startsAt,
            Integer durationMinutes,
            Integer capacity
    ) {
    }

}
