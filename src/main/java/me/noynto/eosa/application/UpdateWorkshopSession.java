package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumSet;

public record UpdateWorkshopSession(
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopBookingProvider workshopBookingProvider,
        Clock clock
) {

    public WorkshopSession handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        if (command.workshopVenueId == null || command.workshopVenueId.value() == null) {
            throw new RuntimeException("Le lieu de la session est requis.");
        }
        if (command.startsAt == null) {
            throw new RuntimeException("La date et l'heure de début sont requises.");
        }
        WorkshopSessionRules.checkCapacity(command.capacity());
        WorkshopSessionRules.checkDuration(command.durationMinutes());

        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        if (workshopSession.getState() != WorkshopSessionState.SCHEDULED) {
            throw new RuntimeException("Une session annulée ne peut plus être modifiée.");
        }
        Instant now = clock.instant();
        if (!workshopSession.getStartsAt().isAfter(now)) {
            throw new RuntimeException("Une session commencée ne peut plus être modifiée.");
        }

        boolean venueChanged = !workshopSession.getVenueId().equals(command.workshopVenueId());
        if (venueChanged) {
            WorkshopVenue workshopVenue = workshopVenueProvider.read(command.workshopVenueId)
                    .orElseThrow(() -> new RuntimeException("Le lieu " + command.workshopVenueId.value() + " n'existe pas."));
            if (workshopVenue.isArchived()) {
                throw new RuntimeException("Le lieu « " + workshopVenue.getName() + " » est archivé.");
            }
        }
        Instant startsAt = command.startsAt().atZone(WorkshopSession.ZONE).toInstant();
        boolean startChanged = !workshopSession.getStartsAt().equals(startsAt);
        if (startChanged && !startsAt.isAfter(now)) {
            throw new RuntimeException("La session doit commencer dans le futur.");
        }

        int occupiedPlaces = (int) workshopBookingProvider.readIds(
                        workshopSession.getId(),
                        EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
                                WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW),
                        null,
                        null
                )
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .filter(booking -> booking.occupiesAPlace(now))
                .count();
        if (occupiedPlaces > 0 && (venueChanged || startChanged)) {
            throw new RuntimeException("Le lieu et la date ne peuvent plus changer : " + occupiedPlaces
                    + " place(s) déjà réservée(s). Annulez la session et créez-en une nouvelle.");
        }
        if (command.capacity() < occupiedPlaces) {
            throw new RuntimeException("La capacité ne peut pas être inférieure aux " + occupiedPlaces + " place(s) déjà réservée(s).");
        }

        workshopSession.setVenueId(command.workshopVenueId());
        workshopSession.setStartsAt(startsAt);
        workshopSession.setDurationMinutes(command.durationMinutes());
        workshopSession.setCapacity(command.capacity());
        return workshopSessionProvider.write(workshopSession);
    }

    /**
     * @param startsAt date et heure de début, en heure de Paris
     */
    public record Command(
            WorkshopSessionId workshopSessionId,
            WorkshopVenueId workshopVenueId,
            LocalDateTime startsAt,
            Integer durationMinutes,
            Integer capacity
    ) {
    }

}
