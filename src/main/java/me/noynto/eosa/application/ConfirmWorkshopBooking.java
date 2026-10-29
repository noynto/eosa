package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

import java.time.Clock;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Confirmation de la place depuis le lien de l'e-mail.
 */
public record ConfirmWorkshopBooking(
        WorkshopBookingProvider workshopBookingProvider,
        SendWorkshopBookingConfirmation sendWorkshopBookingConfirmation,
        Clock clock
) {

    private static final Logger LOGGER = Logger.getLogger(ConfirmWorkshopBooking.class.getName());

    public Outcome handle(Command command) {
        if (command.token == null || command.token.isBlank()) {
            throw new RuntimeException("Le lien de réservation est incomplet.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.readIds(null, null, null, command.token())
                .findFirst()
                .flatMap(workshopBookingProvider::read)
                .orElseThrow(() -> new RuntimeException("Ce lien de réservation n'est pas valide."));

        Instant now = clock.instant();
        switch (workshopBooking.getState()) {
            case CONFIRMED, ATTENDED, NO_SHOW -> {
                return Outcome.ALREADY_CONFIRMED;
            }
            case CANCELLED -> {
                return Outcome.CANCELLED;
            }
            case EXPIRED -> {
                return Outcome.EXPIRED;
            }
            case PENDING -> {
                if (!workshopBooking.isAwaitingConfirmation(now)) {
                    workshopBooking.setState(WorkshopBookingState.EXPIRED);
                    workshopBookingProvider.write(workshopBooking);
                    return Outcome.EXPIRED;
                }
            }
        }

        workshopBooking.setState(WorkshopBookingState.CONFIRMED);
        workshopBooking.setConfirmedAt(now);
        workshopBookingProvider.write(workshopBooking);
        // La place est confirmée même si l'e-mail récapitulatif ne part pas : il sera renvoyé plus tard.
        try {
            sendWorkshopBookingConfirmation.handle(new SendWorkshopBookingConfirmation.Command(workshopBooking.getId()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail récapitulatif de la réservation " + workshopBooking.getId().value()
                    + " n'est pas parti, il sera renvoyé plus tard.", e);
        }
        return Outcome.CONFIRMED;
    }

    public record Command(
            String token
    ) {
    }

    public enum Outcome {
        /** La place vient d'être confirmée. */
        CONFIRMED,
        /** La place était déjà confirmée (lien utilisé deux fois). */
        ALREADY_CONFIRMED,
        /** Le délai de confirmation est dépassé : la place a été libérée. */
        EXPIRED,
        CANCELLED
    }

}
