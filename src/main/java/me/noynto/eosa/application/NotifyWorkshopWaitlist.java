package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopSessionId;
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
 * Prévient toute la liste d'attente qu'une place s'est libérée : la première qui réserve l'obtient.
 * Ne fait rien si la session n'est plus réservable ou si aucune place n'est libre.
 */
public record NotifyWorkshopWaitlist(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(NotifyWorkshopWaitlist.class.getName());

    /**
     * @return le nombre de personnes prévenues
     */
    public int handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        Instant now = clock.instant();
        if (workshopSession.getState() != WorkshopSessionState.SCHEDULED
                || now.isBefore(workshopSession.getBookingOpensAt())
                || !now.isBefore(workshopSession.getStartsAt())) {
            return 0;
        }
        long occupiedPlaces = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
                        WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW), null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .filter(booking -> booking.occupiesAPlace(now))
                .count();
        if (occupiedPlaces >= workshopSession.getCapacity()) {
            return 0;
        }

        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        int notified = 0;
        var waitingIds = workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).toList();
        for (var id : waitingIds) {
            WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(id).orElseThrow();
            Map<String, Object> model = new HashMap<>();
            model.put("preheader", "Une place vient de se libérer pour l'atelier du "
                    + WorkshopDates.dateInSentence(workshopSession.getStartsAt()) + " chez " + workshopVenue.getName() + ".");
            model.put("firstName", entry.getFirstName());
            model.put("venueName", workshopVenue.getName());
            model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
            model.put("timeRange", WorkshopDates.timeRange(workshopSession));
            model.put("sessionUrl", publicBaseUrl + "/workshops/sessions/" + workshopSession.getId().value());
            model.put("leaveUrl", publicBaseUrl + "/workshops/waitlist/" + entry.getToken() + "/leave");
            try {
                mailProvider.send(new Mail(
                        entry.getEmail(),
                        "Une place s'est libérée — Atelier EOSA chez " + workshopVenue.getName(),
                        "workshop-waitlist-place-available",
                        model
                ));
                entry.setLastNotifiedAt(now);
                workshopWaitlistEntryProvider.write(entry);
                notified++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail « Place libérée » n'est pas parti pour l'inscription " + id.value() + ".", e);
            }
        }
        return notified;
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
