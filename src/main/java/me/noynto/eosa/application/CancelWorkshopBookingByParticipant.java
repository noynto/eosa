package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annulation par la participante, depuis le lien de son e-mail : possible jusqu'à 48 h avant le début.
 */
public record CancelWorkshopBookingByParticipant(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        SendWorkshopBookingCancellation sendWorkshopBookingCancellation,
        Clock clock
) {

    private static final Logger LOGGER = Logger.getLogger(CancelWorkshopBookingByParticipant.class.getName());

    public Outcome handle(Command command) {
        if (command.token == null || command.token.isBlank()) {
            throw new RuntimeException("Le lien de réservation est incomplet.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.readIds(null, null, null, command.token())
                .findFirst()
                .flatMap(workshopBookingProvider::read)
                .orElseThrow(() -> new RuntimeException("Ce lien de réservation n'est pas valide."));
        Instant now = clock.instant();
        if (workshopBooking.getState() == WorkshopBookingState.CANCELLED) {
            return Outcome.ALREADY_CANCELLED;
        }
        if (!workshopBooking.isActive(now)) {
            return Outcome.NOT_ACTIVE;
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
        if (!now.isBefore(workshopSession.getStartsAt().minus(WorkshopBooking.SELF_CANCELLATION_DEADLINE))) {
            return Outcome.TOO_LATE;
        }

        workshopBooking.setState(WorkshopBookingState.CANCELLED);
        workshopBooking.setCancelledAt(now);
        workshopBookingProvider.write(workshopBooking);
        // L'annulation est faite même si l'e-mail ne part pas : il sera renvoyé plus tard.
        try {
            sendWorkshopBookingCancellation.handle(new SendWorkshopBookingCancellation.Command(workshopBooking.getId()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail d'annulation de la réservation " + workshopBooking.getId().value()
                    + " n'est pas parti, il sera renvoyé plus tard.", e);
        }
        return Outcome.CANCELLED;
    }

    public record Command(
            String token
    ) {
    }

    public enum Outcome {
        CANCELLED,
        ALREADY_CANCELLED,
        /** Moins de 48 h avant le début : il faut contacter EOSA. */
        TOO_LATE,
        /** Demande jamais confirmée, ou atelier déjà passé : rien à annuler. */
        NOT_ACTIVE
    }

}
