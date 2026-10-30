package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prévient la liste d'attente des sessions dont les réservations viennent d'ouvrir. L'annonce n'est faite
 * qu'une fois par session : la session est marquée annoncée avant les envois, un e-mail en échec n'est
 * pas renvoyé (la personne reste en liste d'attente et sera prévenue si une place se libère).
 */
public record AnnounceWorkshopBookingOpenings(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(AnnounceWorkshopBookingOpenings.class.getName());

    /**
     * @return le nombre de personnes prévenues
     */
    public int handle() {
        Instant now = clock.instant();
        int notified = 0;
        var sessionIds = workshopSessionProvider.readIds().toList();
        for (var sessionId : sessionIds) {
            WorkshopSession workshopSession = workshopSessionProvider.read(sessionId).orElseThrow();
            if (workshopSession.getState() != WorkshopSessionState.SCHEDULED
                    || workshopSession.getOpeningAnnouncedAt() != null
                    || now.isBefore(workshopSession.getBookingOpensAt())
                    || !now.isBefore(workshopSession.getStartsAt())) {
                continue;
            }
            workshopSession.setOpeningAnnouncedAt(now);
            workshopSessionProvider.write(workshopSession);

            // Rien à annoncer si la session est déjà complète : la liste attendra qu'une place se libère.
            long occupiedPlaces = workshopBookingProvider
                    .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
                            WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW), null, null)
                    .map(id -> workshopBookingProvider.read(id).orElseThrow())
                    .filter(booking -> booking.occupiesAPlace(now))
                    .count();
            if (occupiedPlaces >= workshopSession.getCapacity()) {
                continue;
            }

            WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
            var waitingIds = workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).toList();
            for (var entryId : waitingIds) {
                WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(entryId).orElseThrow();
                Map<String, Object> model = new HashMap<>();
                model.put("preheader", "Les réservations sont ouvertes pour l'atelier du "
                        + WorkshopDates.dateInSentence(workshopSession.getStartsAt()) + " chez " + workshopVenue.getName() + ".");
                model.put("firstName", entry.getFirstName());
                model.put("venueName", workshopVenue.getName());
                model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
                model.put("timeRange", WorkshopDates.timeRange(workshopSession));
                model.put("capacity", workshopSession.getCapacity());
                model.put("sessionUrl", publicBaseUrl + "/workshops/sessions/" + workshopSession.getId().value());
                model.put("leaveUrl", publicBaseUrl + "/workshops/waitlist/" + entry.getToken() + "/leave");
                try {
                    mailProvider.send(new Mail(
                            entry.getEmail(),
                            "Les réservations sont ouvertes — Atelier EOSA chez " + workshopVenue.getName(),
                            "workshop-waitlist-booking-open",
                            model
                    ));
                    entry.setLastNotifiedAt(now);
                    workshopWaitlistEntryProvider.write(entry);
                    notified++;
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "L'e-mail d'ouverture des réservations n'est pas parti pour l'inscription "
                            + entryId.value() + ".", e);
                }
            }
        }
        return notified;
    }

}
