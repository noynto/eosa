package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfirmWorkshopBookingTest {

    @Mock SendWorkshopBookingConfirmation sendWorkshopBookingConfirmation;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryWorkshopBookings bookings;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
    }

    @Test
    void handle_confirmsAPendingBookingWithinTheDelayAndSendsTheRecap() {
        var booking = booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(10)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token"));

        assertEquals(ConfirmWorkshopBooking.Outcome.CONFIRMED, outcome);
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
        assertEquals(now, booking.getConfirmedAt());
        verify(sendWorkshopBookingConfirmation).handle(new SendWorkshopBookingConfirmation.Command(booking.getId()));
    }

    @Test
    void handle_keepsTheConfirmationWhenTheRecapCannotBeSent() {
        var booking = booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(10)));
        doThrow(new RuntimeException("SMTP")).when(sendWorkshopBookingConfirmation).handle(any());

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token"));

        assertEquals(ConfirmWorkshopBooking.Outcome.CONFIRMED, outcome);
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
    }

    @Test
    void handle_expiresAPendingBookingPastTheDelay() {
        var booking = booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(31)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token"));

        assertEquals(ConfirmWorkshopBooking.Outcome.EXPIRED, outcome);
        assertEquals(WorkshopBookingState.EXPIRED, booking.getState());
        verify(sendWorkshopBookingConfirmation, never()).handle(any());
    }

    @Test
    void handle_doesNothingMoreWhenAlreadyConfirmed() {
        var booking = booking(WorkshopBookingState.CONFIRMED, now.minus(Duration.ofMinutes(10)));
        booking.setConfirmedAt(now.minus(Duration.ofMinutes(5)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token"));

        assertEquals(ConfirmWorkshopBooking.Outcome.ALREADY_CONFIRMED, outcome);
        assertEquals(now.minus(Duration.ofMinutes(5)), booking.getConfirmedAt());
        verify(sendWorkshopBookingConfirmation, never()).handle(any());
    }

    @Test
    void handle_doesNotConfirmACancelledBooking() {
        var booking = booking(WorkshopBookingState.CANCELLED, now.minus(Duration.ofMinutes(10)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token"));

        assertEquals(ConfirmWorkshopBooking.Outcome.CANCELLED, outcome);
        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
    }

    @Test
    void handle_throwsForAnUnknownToken() {
        booking(WorkshopBookingState.PENDING, now);

        assertThrows(RuntimeException.class, () -> confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("other")));
    }

    private ConfirmWorkshopBooking confirmWorkshopBooking() {
        return new ConfirmWorkshopBooking(bookings, sendWorkshopBookingConfirmation, clock);
    }

    private WorkshopBooking booking(WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("booking"));
        booking.setSessionId(new WorkshopSessionId("session"));
        booking.setEmail("marie@example.com");
        booking.setState(state);
        booking.setToken("token");
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

}
