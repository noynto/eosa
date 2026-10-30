package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.regex.Pattern;

/**
 * Inscription sur la liste d'attente d'une session : avant l'ouverture des réservations (pour être prévenue
 * de l'ouverture), ou quand la session est complète (pour être prévenue si une place se libère).
 */
public record JoinWorkshopWaitlist(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        Clock clock
) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final SecureRandom RANDOM = new SecureRandom();

    public WorkshopWaitlistEntry handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("La session est requise.");
        }
        String firstName = command.firstName() == null || command.firstName().isBlank() ? null : command.firstName().strip();
        String email = command.email() == null || command.email().isBlank() ? null : command.email().strip();
        if (firstName == null || firstName.length() > MAX_NAME_LENGTH) {
            throw new RuntimeException("Votre prénom est requis.");
        }
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new RuntimeException("Une adresse e-mail valide est requise.");
        }

        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("Cette session n'existe pas."));
        Instant now = clock.instant();
        if (workshopSession.getState() != WorkshopSessionState.SCHEDULED) {
            throw new RuntimeException("Cette session a été annulée.");
        }
        if (!now.isBefore(workshopSession.getStartsAt())) {
            throw new RuntimeException("Cet atelier a déjà commencé.");
        }
        boolean bookingOpen = !now.isBefore(workshopSession.getBookingOpensAt());
        long occupiedPlaces = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
                        WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW), null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .filter(booking -> booking.occupiesAPlace(now))
                .count();
        if (bookingOpen && occupiedPlaces < workshopSession.getCapacity()) {
            throw new RuntimeException("Il reste des places : réservez directement la vôtre.");
        }
        boolean missedAWorkshop = workshopBookingProvider
                .readIds(null, EnumSet.of(WorkshopBookingState.NO_SHOW), email, null)
                .findAny()
                .isPresent();
        if (missedAWorkshop) {
            throw new RuntimeException("La réservation en ligne n'est pas possible pour cette adresse : "
                    + "contactez-nous sur Instagram (@eosa.bijoux) pour réserver.");
        }
        boolean alreadyBooked = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED), email, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .anyMatch(booking -> booking.isActive(now));
        if (alreadyBooked) {
            throw new RuntimeException("Vous avez déjà une place (ou une demande en cours) pour cette session.");
        }

        // Déjà inscrite : on garde l'inscription existante, et sa place dans l'ordre d'arrivée.
        var existing = workshopWaitlistEntryProvider.readIds(workshopSession.getId(), email, null, true)
                .findFirst()
                .flatMap(workshopWaitlistEntryProvider::read);
        if (existing.isPresent()) {
            return existing.get();
        }
        WorkshopWaitlistEntry entry = new WorkshopWaitlistEntry();
        entry.setSessionId(workshopSession.getId());
        entry.setFirstName(firstName);
        entry.setEmail(email);
        entry.setToken(newToken());
        entry.setCreatedAt(now);
        return workshopWaitlistEntryProvider.write(entry);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Command(
            WorkshopSessionId workshopSessionId,
            String firstName,
            String email
    ) {
    }

}
