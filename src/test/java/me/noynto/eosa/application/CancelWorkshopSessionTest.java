package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.net.URI;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelWorkshopSessionTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock SendWorkshopBookingCancellation sendWorkshopBookingCancellation;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final WorkshopVenueId venueId = new WorkshopVenueId("venue");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        venue.setName("Café des Arts");
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_closesTheWaitlistAndWarnsEachWaitingPerson() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(sessionStartingAt("2026-11-14T13:00:00Z")));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setFirstName("Rosa");
        entry.setEmail("rosa@example.com");
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        waitlist.write(entry);

        cancelWorkshopSession().handle(new CancelWorkshopSession.Command(sessionId));

        assertEquals(now, entry.getRemovedAt());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("rosa@example.com", mail.getValue().to());
        assertEquals(true, mail.getValue().model().get("waitlisted"));
    }

    @Test
    void handle_cancelsUpcomingSession() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(sessionStartingAt("2026-11-14T13:00:00Z")));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = cancelWorkshopSession().handle(new CancelWorkshopSession.Command(sessionId));

        assertEquals(WorkshopSessionState.CANCELLED, result.getState());
        assertEquals(now, result.getCancelledAt());
    }

    @Test
    void handle_cancelsActiveBookingsAndWarnsEachParticipant() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(sessionStartingAt("2026-11-14T13:00:00Z")));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        var confirmed = booking("confirmed", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));
        var awaiting = booking("awaiting", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(5)));
        var stale = booking("stale", WorkshopBookingState.PENDING, now.minus(Duration.ofHours(2)));
        var expired = booking("expired", WorkshopBookingState.EXPIRED, now.minus(Duration.ofDays(2)));
        // Un e-mail qui échoue ne doit pas empêcher d'annuler et de prévenir les autres.
        doThrow(new RuntimeException("SMTP")).when(sendWorkshopBookingCancellation)
                .handle(new SendWorkshopBookingCancellation.Command(confirmed.getId()));

        cancelWorkshopSession().handle(new CancelWorkshopSession.Command(sessionId));

        assertEquals(WorkshopBookingState.CANCELLED, confirmed.getState());
        assertEquals(WorkshopBookingState.CANCELLED, awaiting.getState());
        assertEquals(now, awaiting.getCancelledAt());
        assertEquals(WorkshopBookingState.PENDING, stale.getState());
        assertEquals(WorkshopBookingState.EXPIRED, expired.getState());
        verify(sendWorkshopBookingCancellation).handle(new SendWorkshopBookingCancellation.Command(confirmed.getId()));
        verify(sendWorkshopBookingCancellation).handle(new SendWorkshopBookingCancellation.Command(awaiting.getId()));
        verifyNoMoreInteractions(sendWorkshopBookingCancellation);
    }

    @Test
    void handle_refusesAlreadyCancelledSession() {
        var session = sessionStartingAt("2026-11-14T13:00:00Z");
        session.setState(WorkshopSessionState.CANCELLED);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> cancelWorkshopSession().handle(new CancelWorkshopSession.Command(sessionId)));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesStartedSession() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(sessionStartingAt("2026-11-01T09:30:00Z")));

        assertThrows(RuntimeException.class, () -> cancelWorkshopSession().handle(new CancelWorkshopSession.Command(sessionId)));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenSessionNotFound() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cancelWorkshopSession().handle(new CancelWorkshopSession.Command(sessionId)));
    }

    private CancelWorkshopSession cancelWorkshopSession() {
        return new CancelWorkshopSession(workshopSessionProvider, workshopVenueProvider, bookings, waitlist,
                sendWorkshopBookingCancellation, mailProvider, clock, URI.create("https://eosa.me"));
    }

    private WorkshopSession sessionStartingAt(String instant) {
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(venueId);
        session.setStartsAt(Instant.parse(instant));
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        return session;
    }

    private WorkshopBooking booking(String id, WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(sessionId);
        booking.setState(state);
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

}
