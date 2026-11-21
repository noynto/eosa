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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RemindWorkshopWaitlistTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    // La session du 28 novembre : ses réservations ont ouvert le 14 novembre.
    private final Instant now = Instant.parse("2026-11-21T12:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;
    private WorkshopSession session;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(Instant.parse("2026-11-28T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(2);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_emailsEveryWaitingPersonAndRecordsTheDate() {
        var rosa = waiting("rosa@example.com");
        var lise = waiting("lise@example.com");
        book("b1");

        var result = remind().handle(new RemindWorkshopWaitlist.Command(sessionId));

        assertEquals(2, result.notified());
        assertEquals(0, result.skipped());
        assertEquals(1, result.freePlaces());
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals("workshop-waitlist-reminder", mails.getAllValues().get(0).template());
        assertEquals("il reste une place", mails.getAllValues().get(0).model().get("placesLabel"));
        assertEquals(now, rosa.getLastNotifiedAt());
        assertEquals(now, lise.getLastNotifiedAt());
    }

    @Test
    void handle_skipsAWaitingPersonWarnedLessThanSixHoursAgo() {
        var recent = waiting("recent@example.com");
        recent.setLastNotifiedAt(now.minus(Duration.ofHours(1)));
        var old = waiting("old@example.com");
        old.setLastNotifiedAt(now.minus(Duration.ofHours(7)));

        var result = remind().handle(new RemindWorkshopWaitlist.Command(sessionId));

        assertEquals(1, result.notified());
        assertEquals(1, result.skipped());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("old@example.com", mail.getValue().to());
        assertEquals(now.minus(Duration.ofHours(1)), recent.getLastNotifiedAt());
    }

    @Test
    void handle_refusesACompleteSession() {
        waiting("rosa@example.com");
        book("b1");
        book("b2");

        assertThrows(RuntimeException.class, () -> remind().handle(new RemindWorkshopWaitlist.Command(sessionId)));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_refusesACancelledSession() {
        waiting("rosa@example.com");
        session.setState(WorkshopSessionState.CANCELLED);

        assertThrows(RuntimeException.class, () -> remind().handle(new RemindWorkshopWaitlist.Command(sessionId)));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_refusesASessionThatHasStarted() {
        waiting("rosa@example.com");
        session.setStartsAt(now.minus(Duration.ofMinutes(5)));

        assertThrows(RuntimeException.class, () -> remind().handle(new RemindWorkshopWaitlist.Command(sessionId)));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_refusesASessionWhoseBookingsAreNotOpenYet() {
        waiting("rosa@example.com");
        session.setStartsAt(Instant.parse("2026-12-20T13:00:00Z"));

        assertThrows(RuntimeException.class, () -> remind().handle(new RemindWorkshopWaitlist.Command(sessionId)));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_refusesAnUnknownSession() {
        when(workshopSessionProvider.read(new WorkshopSessionId("other"))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> remind().handle(new RemindWorkshopWaitlist.Command(new WorkshopSessionId("other"))));
    }

    @Test
    void handle_doesNotCountNorRecordAnEmailThatDoesNotLeave() {
        var rosa = waiting("rosa@example.com");
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        var result = remind().handle(new RemindWorkshopWaitlist.Command(sessionId));

        assertEquals(0, result.notified());
        assertNull(rosa.getLastNotifiedAt());
    }

    private RemindWorkshopWaitlist remind() {
        return new RemindWorkshopWaitlist(workshopSessionProvider, workshopVenueProvider, bookings, waitlist,
                mailProvider, Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me"));
    }

    private WorkshopWaitlistEntry waiting(String email) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setFirstName(email.substring(0, email.indexOf('@')));
        entry.setEmail(email);
        entry.setToken("token-" + email);
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        return waitlist.write(entry);
    }

    private void book(String id) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(sessionId);
        booking.setFirstName("Ada");
        booking.setEmail(id + "@example.com");
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(now.minus(Duration.ofDays(3)));
        bookings.write(booking);
    }

}
