package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.service.WorkshopMails;
import me.noynto.eosa.service.WorkshopPlaces;
import me.noynto.eosa.shared.WorkshopSessionId;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Relance manuelle de la liste d'attente d'une session : un e-mail générique « Il reste des places » à chaque
 * inscrite en attente, tant que la session est ouverte aux réservations et qu'il reste une place. Une inscrite
 * prévenue depuis moins de {@link WorkshopWaitlistEntry#MIN_DELAY_BETWEEN_EMAILS} est sautée.
 */
public record RemindWorkshopWaitlist(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(RemindWorkshopWaitlist.class.getName());

    public Result handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("Cette session n'existe pas."));
        Instant now = clock.instant();
        if (workshopSession.getState() == WorkshopSessionState.CANCELLED) {
            throw new RuntimeException("La session est annulée.");
        }
        if (!now.isBefore(workshopSession.getStartsAt())) {
            throw new RuntimeException("La session a déjà commencé.");
        }
        if (now.isBefore(workshopSession.getBookingOpensAt())) {
            throw new RuntimeException("Les réservations ne sont pas encore ouvertes.");
        }
        var bookings = workshopBookingProvider
                .readIds(workshopSession.getId(), WorkshopPlaces.OCCUPYING_STATES, null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        int freePlaces = WorkshopPlaces.free(workshopSession, bookings, now);
        if (freePlaces == 0) {
            throw new RuntimeException("La session est complète : il n'y a pas de place à annoncer.");
        }

        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        int notified = 0;
        int skipped = 0;
        for (var entryId : workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).toList()) {
            WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(entryId).orElseThrow();
            if (!entry.canBeEmailed(now)) {
                skipped++;
                continue;
            }
            try {
                mailProvider.send(WorkshopMails.waitlistReminder(entry, workshopSession, workshopVenue, freePlaces, publicBaseUrl));
                entry.setLastNotifiedAt(now);
                workshopWaitlistEntryProvider.write(entry);
                notified++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail « Il reste des places » n'est pas parti pour l'inscription " + entryId.value() + ".", e);
            }
        }
        return new Result(notified, skipped, freePlaces);
    }

    /**
     * @param notified   inscrites à qui l'e-mail est parti
     * @param skipped    inscrites sautées parce qu'elles ont reçu un e-mail il y a moins de six heures
     * @param freePlaces places libres de la session au moment de la relance
     */
    public record Result(
            int notified,
            int skipped,
            int freePlaces
    ) {
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
