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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ExpirePendingWorkshopBookingsTest {

    @Mock NotifyWorkshopWaitlist notifyWorkshopWaitlist;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();

    @Test
    void handle_expiresOnlyPendingBookingsPastTheConfirmationDelayAndWarnsTheirWaitlist() {
        var stale = booking("stale", "session-a", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(31)));
        var otherStale = booking("other-stale", "session-a", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(40)));
        var recent = booking("recent", "session-b", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(29)));
        var confirmed = booking("confirmed", "session-b", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));

        int expired = new ExpirePendingWorkshopBookings(bookings, notifyWorkshopWaitlist, Clock.fixed(now, ZoneOffset.UTC)).handle();

        assertEquals(2, expired);
        assertEquals(WorkshopBookingState.EXPIRED, stale.getState());
        assertEquals(WorkshopBookingState.EXPIRED, otherStale.getState());
        assertEquals(WorkshopBookingState.PENDING, recent.getState());
        assertEquals(WorkshopBookingState.CONFIRMED, confirmed.getState());
        // Une seule notification par session dont une place s'est libérée.
        verify(notifyWorkshopWaitlist).handle(new NotifyWorkshopWaitlist.Command(new WorkshopSessionId("session-a")));
        verifyNoMoreInteractions(notifyWorkshopWaitlist);
    }

    private WorkshopBooking booking(String id, String sessionId, WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(new WorkshopSessionId(sessionId));
        booking.setState(state);
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

}
