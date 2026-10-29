package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class RetryWorkshopEmailsTest {

    @Mock SendWorkshopBookingConfirmation sendWorkshopBookingConfirmation;
    @Mock SendWorkshopBookingCancellation sendWorkshopBookingCancellation;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();

    @Test
    void handle_resendsOnlyUnsentRecapsConfirmedMoreThanFiveMinutesAgo() {
        var unsent = confirmed("unsent", now.minus(Duration.ofMinutes(20)), null);
        confirmed("just-confirmed", now.minus(Duration.ofMinutes(2)), null);
        confirmed("already-sent", now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(20)));
        var failing = confirmed("failing", now.minus(Duration.ofHours(1)), null);
        doThrow(new RuntimeException("SMTP")).when(sendWorkshopBookingConfirmation)
                .handle(new SendWorkshopBookingConfirmation.Command(failing.getId()));

        var result = retryWorkshopEmails().handle();

        assertEquals(1, result.sent());
        assertEquals(1, result.failed());
        verify(sendWorkshopBookingConfirmation).handle(new SendWorkshopBookingConfirmation.Command(unsent.getId()));
        verify(sendWorkshopBookingConfirmation).handle(new SendWorkshopBookingConfirmation.Command(failing.getId()));
        verifyNoMoreInteractions(sendWorkshopBookingConfirmation);
    }

    @Test
    void handle_resendsOnlyUnsentCancellationEmailsOlderThanFiveMinutes() {
        var unsent = cancelled("unsent", now.minus(Duration.ofMinutes(20)), null);
        cancelled("just-cancelled", now.minus(Duration.ofMinutes(2)), null);
        cancelled("already-sent", now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(20)));

        var result = retryWorkshopEmails().handle();

        assertEquals(1, result.sent());
        verify(sendWorkshopBookingCancellation).handle(new SendWorkshopBookingCancellation.Command(unsent.getId()));
        verifyNoMoreInteractions(sendWorkshopBookingCancellation);
    }

    private RetryWorkshopEmails retryWorkshopEmails() {
        return new RetryWorkshopEmails(bookings, sendWorkshopBookingConfirmation, sendWorkshopBookingCancellation,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private WorkshopBooking cancelled(String id, Instant cancelledAt, Instant sentAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(new WorkshopSessionId("session"));
        booking.setState(WorkshopBookingState.CANCELLED);
        booking.setCreatedAt(cancelledAt.minus(Duration.ofDays(1)));
        booking.setCancelledAt(cancelledAt);
        booking.setCancellationEmailSentAt(sentAt);
        return bookings.write(booking);
    }

    private WorkshopBooking confirmed(String id, Instant confirmedAt, Instant sentAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(new WorkshopSessionId("session"));
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(confirmedAt.minus(Duration.ofMinutes(1)));
        booking.setConfirmedAt(confirmedAt);
        booking.setConfirmationEmailSentAt(sentAt);
        return bookings.write(booking);
    }

}
