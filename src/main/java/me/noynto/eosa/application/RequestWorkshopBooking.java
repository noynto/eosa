package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Demande de réservation : la place est retenue pendant le délai de confirmation, et la participante
 * reçoit un e-mail pour la confirmer.
 */
public record RequestWorkshopBooking(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9 +().-]{6,30}$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final SecureRandom RANDOM = new SecureRandom();

    public WorkshopBooking handle(Command command) {
        // 1. Vérification de la saisie.
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("La session est requise.");
        }
        String firstName = strip(command.firstName());
        String lastName = strip(command.lastName());
        String email = strip(command.email());
        String phone = strip(command.phone());
        if (firstName == null || firstName.length() > MAX_NAME_LENGTH) {
            throw new RuntimeException("Votre prénom est requis.");
        }
        if (lastName == null || lastName.length() > MAX_NAME_LENGTH) {
            throw new RuntimeException("Votre nom est requis.");
        }
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new RuntimeException("Une adresse e-mail valide est requise.");
        }
        if (phone != null && !PHONE.matcher(phone).matches()) {
            throw new RuntimeException("Le numéro de téléphone est invalide.");
        }
        if (!command.acceptedConditions()) {
            throw new RuntimeException("Merci d'accepter les conditions de réservation.");
        }

        // 2. La session doit être ouverte à la réservation.
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("Cette session n'existe pas."));
        Instant now = clock.instant();
        if (workshopSession.getState() != WorkshopSessionState.SCHEDULED) {
            throw new RuntimeException("Cette session a été annulée.");
        }
        if (now.isBefore(workshopSession.getBookingOpensAt())) {
            throw new RuntimeException("Les réservations ne sont pas encore ouvertes.");
        }
        if (!now.isBefore(workshopSession.getStartsAt())) {
            throw new RuntimeException("Cet atelier a déjà commencé.");
        }
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();

        // 3. Une absence passée bloque la réservation en ligne.
        boolean missedAWorkshop = workshopBookingProvider
                .readIds(null, EnumSet.of(WorkshopBookingState.NO_SHOW), email, null)
                .findAny()
                .isPresent();
        if (missedAWorkshop) {
            throw new RuntimeException("La réservation en ligne n'est pas possible pour cette adresse : "
                    + "contactez-nous sur Instagram (@eosa.bijoux) pour réserver.");
        }

        // 4. Une seule réservation active par adresse sur la session ; une demande non confirmée à temps
        //    n'en est plus une.
        List<WorkshopBooking> activeBookings = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED), email, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        for (WorkshopBooking activeBooking : activeBookings) {
            if (activeBooking.getState() == WorkshopBookingState.PENDING && !activeBooking.isAwaitingConfirmation(now)) {
                activeBooking.setState(WorkshopBookingState.EXPIRED);
                workshopBookingProvider.write(activeBooking);
            } else if (activeBooking.getState() == WorkshopBookingState.PENDING) {
                throw new RuntimeException("Une demande est déjà en cours pour cette adresse : "
                        + "confirmez-la grâce à l'e-mail reçu (pensez à regarder dans les spams).");
            } else {
                throw new RuntimeException("Vous avez déjà une place réservée pour cette session.");
            }
        }

        // 5. Réservation de la place.
        WorkshopBooking workshopBooking = new WorkshopBooking();
        workshopBooking.setSessionId(workshopSession.getId());
        workshopBooking.setFirstName(firstName);
        workshopBooking.setLastName(lastName);
        workshopBooking.setEmail(email);
        workshopBooking.setPhone(phone);
        workshopBooking.setState(WorkshopBookingState.PENDING);
        workshopBooking.setToken(newToken());
        workshopBooking.setCreatedAt(now);
        workshopBooking = workshopBookingProvider.write(workshopBooking);

        // 6. Demandes simultanées : la réservation est enregistrée avant d'être recomptée. Les réservations
        //    les plus récentes au-delà de la capacité se retirent d'elles-mêmes ; chaque demande faisant le
        //    même calcul, il ne peut pas y avoir plus d'inscrites que de places.
        List<WorkshopBookingId> occupyingIds = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
                        WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW), null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .filter(booking -> booking.occupiesAPlace(now))
                .map(WorkshopBooking::getId)
                .toList();
        if (occupyingIds.indexOf(workshopBooking.getId()) >= workshopSession.getCapacity()) {
            workshopBooking.setState(WorkshopBookingState.EXPIRED);
            workshopBookingProvider.write(workshopBooking);
            throw new RuntimeException("Désolée, cette session est complète.");
        }

        // 7. E-mail de confirmation : sans lui, la place ne pourrait jamais être confirmée.
        try {
            mailProvider.send(confirmationRequestMail(workshopBooking, workshopSession, workshopVenue));
        } catch (RuntimeException e) {
            workshopBooking.setState(WorkshopBookingState.EXPIRED);
            workshopBookingProvider.write(workshopBooking);
            throw new RuntimeException("L'e-mail de confirmation n'a pas pu être envoyé à " + email
                    + ". Vérifiez l'adresse et réessayez.", e);
        }
        return workshopBooking;
    }

    private Mail confirmationRequestMail(WorkshopBooking workshopBooking, WorkshopSession workshopSession,
                                         WorkshopVenue workshopVenue) {
        String venueAddress = workshopVenue.getAddress() + ", " + workshopVenue.getPostalCode() + " " + workshopVenue.getCity();
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Confirmez votre place à l'atelier du " + WorkshopDates.dateInSentence(workshopSession.getStartsAt())
                + " chez " + workshopVenue.getName() + ".");
        model.put("firstName", workshopBooking.getFirstName());
        model.put("venueName", workshopVenue.getName());
        model.put("venueAddress", venueAddress);
        model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
        model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(workshopSession));
        model.put("confirmUrl", publicBaseUrl + "/workshops/bookings/" + workshopBooking.getToken() + "/confirm");
        model.put("confirmationDelayMinutes", WorkshopBooking.CONFIRMATION_DELAY.toMinutes());
        return new Mail(
                workshopBooking.getEmail(),
                "Confirmez votre place — Atelier EOSA chez " + workshopVenue.getName(),
                "workshop-booking-request",
                model
        );
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String strip(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public record Command(
            WorkshopSessionId workshopSessionId,
            String firstName,
            String lastName,
            String email,
            String phone,
            boolean acceptedConditions
    ) {
    }

}
