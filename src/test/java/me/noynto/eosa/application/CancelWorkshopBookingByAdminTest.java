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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelWorkshopBookingByAdminTest {

    @Mock SendWorkshopBookingCancellation sendWorkshopBookingCancellation;
    @Mock NotifyWorkshopWaitlist notifyWorkshopWaitlist;

    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private WorkshopBooking booking;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("booking"));
        booking.setSessionId(sessionId);
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(now.minus(Duration.ofDays(3)));
        bookings.write(booking);
    }

    @Test
    void handle_cancelsWithoutDeadlineAndSendsTheEmail() {
        // Quelques heures avant l'atelier : seule l'administration peut encore annuler.
        cancelWorkshopBookingByAdmin().handle(new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId()));

        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
        assertEquals(now, booking.getCancelledAt());
        verify(sendWorkshopBookingCancellation).handle(new SendWorkshopBookingCancellation.Command(booking.getId()));
        verify(notifyWorkshopWaitlist).handle(new NotifyWorkshopWaitlist.Command(sessionId));
    }

    @Test
    void handle_refusesABookingOfAnotherSession() {
        assertThrows(RuntimeException.class, () -> cancelWorkshopBookingByAdmin().handle(
                new CancelWorkshopBookingByAdmin.Command(new WorkshopSessionId("other"), booking.getId())));
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
    }

    @Test
    void handle_refusesAnInactiveBooking() {
        booking.setState(WorkshopBookingState.CANCELLED);

        assertThrows(RuntimeException.class, () -> cancelWorkshopBookingByAdmin().handle(
                new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId())));
        verify(sendWorkshopBookingCancellation, never()).handle(any());
    }

    private CancelWorkshopBookingByAdmin cancelWorkshopBookingByAdmin() {
        return new CancelWorkshopBookingByAdmin(bookings, sendWorkshopBookingCancellation, notifyWorkshopWaitlist,
                Clock.fixed(now, ZoneOffset.UTC));
    }

}
