package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.service.WorkshopMails;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renvoie les e-mails récapitulatifs et d'annulation qui ne sont pas partis sur le moment.
 */
public record RetryWorkshopEmails(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    /**
     * Laisse le temps à l'envoi immédiat d'avoir lieu, pour ne pas envoyer deux fois le même e-mail.
     */
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(5);
    private static final Logger LOGGER = Logger.getLogger(RetryWorkshopEmails.class.getName());

    public Result handle() {
        Instant now = clock.instant();
        Instant retryBefore = now.minus(GRACE_PERIOD);
        int sent = 0;
        int failed = 0;
        var confirmedIds = workshopBookingProvider.readIds(null, EnumSet.of(WorkshopBookingState.CONFIRMED), null, null).toList();
        for (var id : confirmedIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            Instant confirmedAt = workshopBooking.getConfirmedAt() != null ? workshopBooking.getConfirmedAt() : workshopBooking.getCreatedAt();
            if (workshopBooking.getConfirmationEmailSentAt() != null || !confirmedAt.isBefore(retryBefore)) {
                continue;
            }
            try {
                WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
                WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
                mailProvider.send(WorkshopMails.bookingConfirmation(workshopBooking, workshopSession, workshopVenue, publicBaseUrl));
                workshopBooking.setConfirmationEmailSentAt(now);
                workshopBookingProvider.write(workshopBooking);
                sent++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail récapitulatif de la réservation " + id.value() + " n'est toujours pas parti.", e);
                failed++;
            }
        }
        var cancelledIds = workshopBookingProvider.readIds(null, EnumSet.of(WorkshopBookingState.CANCELLED), null, null).toList();
        for (var id : cancelledIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            if (workshopBooking.getCancellationEmailSentAt() != null
                    || workshopBooking.getCancelledAt() == null
                    || !workshopBooking.getCancelledAt().isBefore(retryBefore)) {
                continue;
            }
            try {
                WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
                WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
                mailProvider.send(WorkshopMails.bookingCancellation(workshopBooking, workshopSession, workshopVenue, publicBaseUrl));
                workshopBooking.setCancellationEmailSentAt(now);
                workshopBookingProvider.write(workshopBooking);
                sent++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail d'annulation de la réservation " + id.value() + " n'est toujours pas parti.", e);
                failed++;
            }
        }
        return new Result(sent, failed);
    }

    public record Result(
            int sent,
            int failed
    ) {
    }

}
