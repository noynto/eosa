package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendWorkshopRemindersTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant startsAt = Instant.parse("2026-11-14T13:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private WorkshopSession session;
    private WorkshopBooking booking;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        venue.setAddress("12 rue Stanislas");
        venue.setPostalCode("54000");
        venue.setCity("Nancy");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
        booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("booking"));
        booking.setSessionId(sessionId);
        booking.setFirstName("Marie");
        booking.setEmail("marie@example.com");
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(startsAt.minus(Duration.ofDays(5)));
        booking.setConfirmedAt(startsAt.minus(Duration.ofDays(5)));
        bookings.write(booking);
    }

    @Test
    void handle_sendsTheReminderOnceWithin24HoursOfTheStart() {
        var now = startsAt.minus(Duration.ofHours(23));

        var result = sendAt(now);
        var again = sendAt(now.plus(Duration.ofMinutes(15)));

        assertEquals(1, result.sent());
        assertEquals(0, again.sent());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("workshop-booking-reminder", mail.getValue().template());
        assertEquals("marie@example.com", mail.getValue().to());
        assertEquals(now, booking.getReminderEmailSentAt());
    }

    @Test
    void handle_waitsUntil24HoursBeforeTheStart() {
        assertEquals(0, sendAt(startsAt.minus(Duration.ofHours(25))).sent());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_skipsAStartedSession() {
        assertEquals(0, sendAt(startsAt.plus(Duration.ofMinutes(1))).sent());
    }

    @Test
    void handle_skipsAPlaceConfirmedLessThan24HoursBeforeTheStart() {
        booking.setConfirmedAt(startsAt.minus(Duration.ofHours(10)));

        assertEquals(0, sendAt(startsAt.minus(Duration.ofHours(9))).sent());
    }

    @Test
    void handle_skipsACancelledSession() {
        session.setState(WorkshopSessionState.CANCELLED);

        assertEquals(0, sendAt(startsAt.minus(Duration.ofHours(23))).sent());
    }

    @Test
    void handle_retriesLaterWhenSendingFails() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        var result = sendAt(startsAt.minus(Duration.ofHours(23)));

        assertEquals(1, result.failed());
        assertNull(booking.getReminderEmailSentAt());
    }

    private SendWorkshopReminders.Result sendAt(Instant now) {
        return new SendWorkshopReminders(bookings, workshopSessionProvider, workshopVenueProvider, mailProvider,
                Clock.fixed(now, ZoneOffset.UTC)).handle();
    }

}
