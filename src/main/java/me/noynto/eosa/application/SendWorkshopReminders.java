package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envoie le rappel « C'est demain ! » aux inscrites confirmées des sessions qui commencent dans les 24 h,
 * une seule fois par réservation. Une place confirmée moins de 24 h avant le début n'en reçoit pas :
 * le récapitulatif vient de partir.
 */
public record SendWorkshopReminders(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        MailProvider mailProvider,
        Clock clock
) {

    public static final Duration REMINDER_BEFORE_START = Duration.ofHours(24);
    private static final Logger LOGGER = Logger.getLogger(SendWorkshopReminders.class.getName());

    public Result handle() {
        Instant now = clock.instant();
        Map<WorkshopSessionId, WorkshopSession> sessions = new HashMap<>();
        int sent = 0;
        int failed = 0;
        var confirmedIds = workshopBookingProvider.readIds(null, EnumSet.of(WorkshopBookingState.CONFIRMED), null, null).toList();
        for (var id : confirmedIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            if (workshopBooking.getReminderEmailSentAt() != null) {
                continue;
            }
            WorkshopSession workshopSession = sessions.computeIfAbsent(workshopBooking.getSessionId(),
                    sessionId -> workshopSessionProvider.read(sessionId).orElseThrow());
            Instant reminderTime = workshopSession.getStartsAt().minus(REMINDER_BEFORE_START);
            Instant confirmedAt = workshopBooking.getConfirmedAt() != null ? workshopBooking.getConfirmedAt() : workshopBooking.getCreatedAt();
            if (workshopSession.getState() != WorkshopSessionState.SCHEDULED
                    || now.isBefore(reminderTime)
                    || !now.isBefore(workshopSession.getStartsAt())
                    || !confirmedAt.isBefore(reminderTime)) {
                continue;
            }

            WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
            String venueAddress = workshopVenue.getAddress() + ", " + workshopVenue.getPostalCode() + " " + workshopVenue.getCity();
            Map<String, Object> model = new HashMap<>();
            model.put("preheader", "Rendez-vous demain à " + WorkshopDates.time(workshopSession.getStartsAt())
                    + " chez " + workshopVenue.getName() + ".");
            model.put("firstName", workshopBooking.getFirstName());
            model.put("venueName", workshopVenue.getName());
            model.put("venueAddress", venueAddress);
            model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
            model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
            model.put("timeRange", WorkshopDates.timeRange(workshopSession));
            try {
                mailProvider.send(new Mail(
                        workshopBooking.getEmail(),
                        "C'est demain ! — Atelier EOSA chez " + workshopVenue.getName(),
                        "workshop-booking-reminder",
                        model
                ));
                workshopBooking.setReminderEmailSentAt(now);
                workshopBookingProvider.write(workshopBooking);
                sent++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Le rappel de la réservation " + id.value() + " n'est pas parti, il sera retenté.", e);
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
