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
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpirePendingWorkshopBookingsTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();
    private final InMemoryWorkshopWaitlistEntries waitlist = new InMemoryWorkshopWaitlistEntries();

    @BeforeEach
    void setUp() {
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        when(workshopVenueProvider.read(new WorkshopVenueId("venue"))).thenReturn(Optional.of(venue));
        session("session-a");
        session("session-b");
    }

    @Test
    void handle_expiresOnlyPendingBookingsPastTheConfirmationDelayAndWarnsTheirWaitlist() {
        var stale = booking("stale", "session-a", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(31)));
        var otherStale = booking("other-stale", "session-a", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(40)));
        var recent = booking("recent", "session-b", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(29)));
        var confirmed = booking("confirmed", "session-b", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));
        var rosa = waiting("rosa@example.com", "session-a");
        var lise = waiting("lise@example.com", "session-b");

        int expired = expirePendingWorkshopBookings().handle();

        assertEquals(2, expired);
        assertEquals(WorkshopBookingState.EXPIRED, stale.getState());
        assertEquals(WorkshopBookingState.EXPIRED, otherStale.getState());
        assertEquals(WorkshopBookingState.PENDING, recent.getState());
        assertEquals(WorkshopBookingState.CONFIRMED, confirmed.getState());
        // Une seule notification par session dont une demande a expiré : « session-b » n'a rien perdu.
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(1)).send(mails.capture());
        assertEquals("rosa@example.com", mails.getValue().to());
        assertEquals("workshop-waitlist-place-available", mails.getValue().template());
        assertEquals(now, rosa.getLastNotifiedAt());
        assertNull(lise.getLastNotifiedAt());
    }

    private void session(String id) {
        var session = new WorkshopSession();
        session.setId(new WorkshopSessionId(id));
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(2);
        session.setState(WorkshopSessionState.SCHEDULED);
        when(workshopSessionProvider.read(session.getId())).thenReturn(Optional.of(session));
    }

    private ExpirePendingWorkshopBookings expirePendingWorkshopBookings() {
        return new ExpirePendingWorkshopBookings(bookings, workshopSessionProvider, workshopVenueProvider, waitlist,
                mailProvider, Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me"));
    }

    private WorkshopBooking booking(String id, String sessionId, WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(new WorkshopSessionId(sessionId));
        booking.setState(state);
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

    private WorkshopWaitlistEntry waiting(String email, String sessionId) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(new WorkshopSessionId(sessionId));
        entry.setFirstName(email.substring(0, email.indexOf('@')));
        entry.setEmail(email);
        entry.setToken("token-" + entry.getFirstName());
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        return waitlist.write(entry);
    }

}
