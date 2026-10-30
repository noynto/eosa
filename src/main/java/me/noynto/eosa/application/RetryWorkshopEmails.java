package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

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
        SendWorkshopBookingConfirmation sendWorkshopBookingConfirmation,
        SendWorkshopBookingCancellation sendWorkshopBookingCancellation,
        Clock clock
) {

    /**
     * Laisse le temps à l'envoi immédiat d'avoir lieu, pour ne pas envoyer deux fois le même e-mail.
     */
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(5);
    private static final Logger LOGGER = Logger.getLogger(RetryWorkshopEmails.class.getName());

    public Result handle() {
        Instant retryBefore = clock.instant().minus(GRACE_PERIOD);
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
                sendWorkshopBookingConfirmation.handle(new SendWorkshopBookingConfirmation.Command(id));
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
                sendWorkshopBookingCancellation.handle(new SendWorkshopBookingCancellation.Command(id));
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
