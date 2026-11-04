package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadWorkshopBookingsTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();

    @Test
    void handle_returnsTheSessionAndItsBookingsWithoutTheExpiredRequests() {
        var session = new WorkshopSession();
        session.setId(sessionId);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        booking("confirmed", sessionId, WorkshopBookingState.CONFIRMED, 3);
        booking("cancelled", sessionId, WorkshopBookingState.CANCELLED, 2);
        booking("expired", sessionId, WorkshopBookingState.EXPIRED, 1);
        booking("other-session", new WorkshopSessionId("other"), WorkshopBookingState.CONFIRMED, 1);

        var result = new ReadWorkshopBookings(bookings, workshopSessionProvider).handle(new ReadWorkshopBookings.Command(sessionId));

        assertSame(session, result.session());
        assertEquals(List.of("confirmed", "cancelled"), result.bookings().stream().map(booking -> booking.getId().value()).toList());
    }

    @Test
    void handle_throwsWhenTheSessionDoesNotExist() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ReadWorkshopBookings(bookings, workshopSessionProvider).handle(new ReadWorkshopBookings.Command(sessionId)));
    }

    private void booking(String id, WorkshopSessionId bookingSessionId, WorkshopBookingState state, int daysAgo) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(bookingSessionId);
        booking.setState(state);
        booking.setCreatedAt(now.minus(Duration.ofDays(daysAgo)));
        bookings.write(booking);
    }

}
