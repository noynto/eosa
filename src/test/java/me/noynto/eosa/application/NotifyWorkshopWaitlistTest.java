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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotifyWorkshopWaitlistTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
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
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(2);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("ada"));
        booking.setSessionId(sessionId);
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setCreatedAt(now.minus(Duration.ofDays(1)));
        bookings.write(booking);
        rosa = entry("rosa@example.com", "token-rosa", null);
        lise = entry("lise@example.com", "token-lise", null);
        entry("gone@example.com", "token-gone", now.minus(Duration.ofHours(1)));
    }

    @Test
    void handle_warnsEveryWaitingPersonWhenAPlaceIsFree() {
        int notified = notifyWorkshopWaitlist().handle(new NotifyWorkshopWaitlist.Command(sessionId));

        assertEquals(2, notified);
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals("rosa@example.com", mails.getAllValues().getFirst().to());
        assertEquals("https://eosa.me/workshops/sessions/session", mails.getAllValues().getFirst().model().get("sessionUrl"));
        assertEquals("https://eosa.me/workshops/waitlist/token-rosa/leave", mails.getAllValues().getFirst().model().get("leaveUrl"));
        assertEquals(now, rosa.getLastNotifiedAt());
        assertEquals(now, lise.getLastNotifiedAt());
    }

    @Test
    void handle_warnsNobodyWhenTheSessionIsStillFull() {
        session.setCapacity(1);

        assertEquals(0, notifyWorkshopWaitlist().handle(new NotifyWorkshopWaitlist.Command(sessionId)));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_warnsNobodyOnceTheSessionHasStarted() {
        session.setStartsAt(now.minus(Duration.ofMinutes(5)));

        assertEquals(0, notifyWorkshopWaitlist().handle(new NotifyWorkshopWaitlist.Command(sessionId)));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_keepsWarningTheOthersWhenAnEmailFails() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(org.mockito.ArgumentMatchers.argThat(mail -> mail.to().startsWith("rosa")));

        int notified = notifyWorkshopWaitlist().handle(new NotifyWorkshopWaitlist.Command(sessionId));

        assertEquals(1, notified);
        assertNull(rosa.getLastNotifiedAt());
        assertEquals(now, lise.getLastNotifiedAt());
    }

    private NotifyWorkshopWaitlist notifyWorkshopWaitlist() {
        return new NotifyWorkshopWaitlist(workshopSessionProvider, workshopVenueProvider, bookings, waitlist, mailProvider,
                Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me"));
    }

    private WorkshopWaitlistEntry entry(String email, String token, Instant removedAt) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setFirstName(email.substring(0, email.indexOf('@')));
        entry.setEmail(email);
        entry.setToken(token);
        // Inscriptions espacées d'une minute, dans l'ordre de création.
        entry.setCreatedAt(now.minus(Duration.ofDays(1)).plus(Duration.ofMinutes(waitlist.all().size())));
        entry.setRemovedAt(removedAt);
        return waitlist.write(entry);
    }

}
