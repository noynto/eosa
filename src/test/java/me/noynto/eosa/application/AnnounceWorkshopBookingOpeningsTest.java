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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnounceWorkshopBookingOpeningsTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    // Samedi 14 novembre 2026, 14 h 00 à Paris : réservations ouvertes le samedi 7 à 14 h 00.
    private final Instant startsAt = Instant.parse("2026-11-14T13:00:00Z");
    private final Instant opening = Instant.parse("2026-11-07T13:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;
    private WorkshopSession session;
    private WorkshopWaitlistEntry rosa;
    private WorkshopWaitlistEntry lise;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        session.setCapacity(8);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        when(workshopSessionProvider.readIds()).thenAnswer(inv -> Stream.of(sessionId));
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
        rosa = entry("rosa@example.com", 0);
        lise = entry("lise@example.com", 1);
    }

    @Test
    void handle_warnsEveryWaitingPersonOnceWhenBookingsOpen() {
        var now = opening.plus(Duration.ofSeconds(40));

        int notified = announceAt(now);
        int again = announceAt(now.plus(Duration.ofMinutes(1)));

        assertEquals(2, notified);
        assertEquals(0, again);
        assertEquals(now, session.getOpeningAnnouncedAt());
        assertEquals(now, rosa.getLastNotifiedAt());
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals("workshop-waitlist-booking-open", mails.getValue().template());
        assertEquals("https://eosa.me/workshops/sessions/session", mails.getValue().model().get("sessionUrl"));
    }

    @Test
    void handle_waitsForTheOpening() {
        assertEquals(0, announceAt(opening.minus(Duration.ofMinutes(1))));
        assertNull(session.getOpeningAnnouncedAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_skipsACancelledSession() {
        session.setState(WorkshopSessionState.CANCELLED);

        assertEquals(0, announceAt(opening.plus(Duration.ofMinutes(1))));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_announcesNothingWhenTheSessionIsAlreadyFull() {
        session.setCapacity(1);
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("ada"));
        booking.setSessionId(sessionId);
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(opening);
        bookings.write(booking);

        assertEquals(0, announceAt(opening.plus(Duration.ofMinutes(1))));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_doesNotResendWhenAnEmailFails() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider)
                .send(org.mockito.ArgumentMatchers.argThat(mail -> mail.to().startsWith("rosa")));

        int notified = announceAt(opening.plus(Duration.ofMinutes(1)));
        int again = announceAt(opening.plus(Duration.ofMinutes(2)));

        assertEquals(1, notified);
        assertEquals(0, again);
        assertNull(rosa.getLastNotifiedAt());
    }

    private int announceAt(Instant now) {
        return new AnnounceWorkshopBookingOpenings(workshopSessionProvider, workshopVenueProvider, bookings, waitlist,
                mailProvider, Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me")).handle();
    }

    private WorkshopWaitlistEntry entry(String email, int order) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setFirstName(email.substring(0, email.indexOf('@')));
        entry.setEmail(email);
        entry.setToken("token-" + email);
        entry.setCreatedAt(opening.minus(Duration.ofDays(2)).plus(Duration.ofMinutes(order)));
        return waitlist.write(entry);
    }

}
