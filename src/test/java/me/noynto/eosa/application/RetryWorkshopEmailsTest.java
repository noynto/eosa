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

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetryWorkshopEmailsTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();

    @BeforeEach
    void setUp() {
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        venue.setAddress("12 rue des Arts");
        venue.setPostalCode("54000");
        venue.setCity("Nancy");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_resendsOnlyUnsentRecapsConfirmedMoreThanFiveMinutesAgo() {
        var unsent = confirmed("unsent", now.minus(Duration.ofMinutes(20)), null);
        confirmed("just-confirmed", now.minus(Duration.ofMinutes(2)), null);
        confirmed("already-sent", now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(20)));
        var failing = confirmed("failing", now.minus(Duration.ofHours(1)), null);
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(argThat(mail -> mail.to().equals("failing@example.com")));

        var result = retryWorkshopEmails().handle();

        assertEquals(1, result.sent());
        assertEquals(1, result.failed());
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        // Les plus anciennes réservations d'abord.
        assertEquals(List.of("failing@example.com", "unsent@example.com"), mails.getAllValues().stream().map(Mail::to).toList());
        assertEquals("workshop-booking-confirmation", mails.getAllValues().getFirst().template());
        assertEquals(now, unsent.getConfirmationEmailSentAt());
        assertNull(failing.getConfirmationEmailSentAt());
    }

    @Test
    void handle_resendsOnlyUnsentCancellationEmailsOlderThanFiveMinutes() {
        var unsent = cancelled("unsent", now.minus(Duration.ofMinutes(20)), null);
        cancelled("just-cancelled", now.minus(Duration.ofMinutes(2)), null);
        cancelled("already-sent", now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(20)));

        var result = retryWorkshopEmails().handle();

        assertEquals(1, result.sent());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("unsent@example.com", mail.getValue().to());
        assertEquals("workshop-booking-cancellation", mail.getValue().template());
        assertEquals(now, unsent.getCancellationEmailSentAt());
    }

    private RetryWorkshopEmails retryWorkshopEmails() {
        return new RetryWorkshopEmails(bookings, workshopSessionProvider, workshopVenueProvider, mailProvider,
                Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me"));
    }

    private WorkshopBooking cancelled(String id, Instant cancelledAt, Instant sentAt) {
        var booking = booking(id);
        booking.setState(WorkshopBookingState.CANCELLED);
        booking.setCreatedAt(cancelledAt.minus(Duration.ofDays(1)));
        booking.setCancelledAt(cancelledAt);
        booking.setCancellationEmailSentAt(sentAt);
        return bookings.write(booking);
    }

    private WorkshopBooking confirmed(String id, Instant confirmedAt, Instant sentAt) {
        var booking = booking(id);
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(confirmedAt.minus(Duration.ofMinutes(1)));
        booking.setConfirmedAt(confirmedAt);
        booking.setConfirmationEmailSentAt(sentAt);
        return bookings.write(booking);
    }

    private WorkshopBooking booking(String id) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(sessionId);
        booking.setFirstName(id);
        booking.setEmail(id + "@example.com");
        booking.setToken("token-" + id);
        return booking;
    }

}
