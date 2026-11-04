package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.service.WorkshopMails;
import me.noynto.eosa.service.WorkshopPlaces;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public record UpdateWorkshopSession(
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(UpdateWorkshopSession.class.getName());

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

        var bookings = workshopBookingProvider
                .readIds(workshopSession.getId(), WorkshopPlaces.OCCUPYING_STATES, null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        int occupiedPlaces = WorkshopPlaces.occupied(bookings, now);
        if (occupiedPlaces > 0 && (venueChanged || startChanged)) {
            throw new RuntimeException("Le lieu et la date ne peuvent plus changer : " + occupiedPlaces
                    + " place(s) déjà réservée(s). Annulez la session et créez-en une nouvelle.");
        }
        if (command.capacity() < occupiedPlaces) {
            throw new RuntimeException("La capacité ne peut pas être inférieure aux " + occupiedPlaces + " place(s) déjà réservée(s).");
        }

        boolean capacityIncreased = command.capacity() > workshopSession.getCapacity();
        workshopSession.setVenueId(command.workshopVenueId());
        workshopSession.setStartsAt(startsAt);
        workshopSession.setDurationMinutes(command.durationMinutes());
        workshopSession.setCapacity(command.capacity());
        workshopSessionProvider.write(workshopSession);
        // De nouvelles places : la liste d'attente est prévenue.
        if (capacityIncreased) {
            try {
                notifyWaitlist(workshopSession, bookings, now);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "La liste d'attente de la session " + workshopSession.getId().value()
                        + " n'a pas pu être prévenue.", e);
            }
        }
        return workshopSession;
    }

    /**
     * Les nouvelles places sont annoncées à toute la liste d'attente : la première personne qui réserve les obtient.
     */
    private void notifyWaitlist(WorkshopSession workshopSession, List<WorkshopBooking> bookings, Instant now) {
        if (!workshopSession.isBookable(now) || WorkshopPlaces.free(workshopSession, bookings, now) == 0) {
            return;
        }
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        for (var entryId : workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).toList()) {
            WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(entryId).orElseThrow();
            try {
                mailProvider.send(WorkshopMails.waitlistPlaceAvailable(entry, workshopSession, workshopVenue, publicBaseUrl));
                entry.setLastNotifiedAt(now);
                workshopWaitlistEntryProvider.write(entry);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail « Place libérée » n'est pas parti pour l'inscription " + entryId.value() + ".", e);
            }
        }
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
