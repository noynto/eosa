package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateWorkshopSessionTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopBookingProvider workshopBookingProvider;
    @Mock MailProvider mailProvider;

    private final InMemoryWorkshopWaitlistEntries waitlist = new InMemoryWorkshopWaitlistEntries();

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final WorkshopVenueId venueId = new WorkshopVenueId("venue");
    private final WorkshopVenueId otherVenueId = new WorkshopVenueId("other-venue");
    // 14 novembre 2026, 14 h 00 à Paris.
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 11, 14, 14, 0);

    @Test
    void handle_updatesEverythingWhenNoPlaceIsBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(otherVenueId)).thenReturn(Optional.of(venue(otherVenueId, false)));
        bookedPlaces(0);
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, otherVenueId, LocalDateTime.of(2026, 11, 21, 10, 30), 90, 12
        ));

        assertEquals(otherVenueId, result.getVenueId());
        assertEquals(Instant.parse("2026-11-21T09:30:00Z"), result.getStartsAt());
        assertEquals(90, result.getDurationMinutes());
        assertEquals(12, result.getCapacity());
        // Les réservations n'ouvrent que le 14 novembre pour une session du 21 : personne à prévenir.
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_warnsTheWaitingListWhenPlacesAreAddedToABookableSession() {
        var now = Instant.parse("2026-11-10T10:00:00Z");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue(venueId, false)));
        bookedPlaces(3);
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        var rosa = new WorkshopWaitlistEntry();
        rosa.setSessionId(sessionId);
        rosa.setFirstName("Rosa");
        rosa.setEmail("rosa@example.com");
        rosa.setToken("token-rosa");
        rosa.setCreatedAt(now.minus(Duration.ofDays(1)));
        waitlist.write(rosa);

        // 8 → 12 places : la liste d'attente est prévenue.
        updateWorkshopSession(Clock.fixed(now, ZoneOffset.UTC)).handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 12
        ));

        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("rosa@example.com", mail.getValue().to());
        assertEquals("workshop-waitlist-place-available", mail.getValue().template());
        assertEquals(now, rosa.getLastNotifiedAt());
    }

    @Test
    void handle_warnsNobodyWhenTheNewCapacityIsAlreadyFull() {
        var now = Instant.parse("2026-11-10T10:00:00Z");
        var session = scheduledSession();
        session.setCapacity(3);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        bookedPlaces(4);
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        updateWorkshopSession(Clock.fixed(now, ZoneOffset.UTC)).handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 4
        ));

        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_allowsDurationAndCapacityChangesWhenPlacesAreBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        bookedPlaces(3);
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 150, 3
        ));

        assertEquals(150, result.getDurationMinutes());
        assertEquals(3, result.getCapacity());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_refusesDateChangeWhenPlacesAreBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        bookedPlaces(1);

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt.plusDays(1), 120, 8
        )));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesVenueChangeWhenPlacesAreBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(otherVenueId)).thenReturn(Optional.of(venue(otherVenueId, false)));
        bookedPlaces(1);

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, otherVenueId, startsAt, 120, 8
        )));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesCapacityBelowBookedPlaces() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        bookedPlaces(5);

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 4
        )));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesArchivedVenue() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(otherVenueId)).thenReturn(Optional.of(venue(otherVenueId, true)));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, otherVenueId, startsAt, 120, 8
        )));
    }

    @Test
    void handle_refusesNewStartInThePast() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, LocalDateTime.of(2026, 10, 30, 14, 0), 120, 8
        )));
    }

    @Test
    void handle_refusesCancelledSession() {
        var session = scheduledSession();
        session.setState(WorkshopSessionState.CANCELLED);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 8
        )));
    }

    @Test
    void handle_refusesStartedSession() {
        var session = scheduledSession();
        session.setStartsAt(Instant.parse("2026-11-01T09:00:00Z"));
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 8
        )));
    }

    private void bookedPlaces(int count) {
        List<WorkshopBookingId> ids = IntStream.range(0, count)
                .mapToObj(i -> new WorkshopBookingId("booking-" + i))
                .toList();
        when(workshopBookingProvider.readIds(eq(sessionId), any(), isNull(), isNull())).thenReturn(ids.stream());
        ids.forEach(id -> {
            var booking = new WorkshopBooking();
            booking.setId(id);
            booking.setState(WorkshopBookingState.CONFIRMED);
            when(workshopBookingProvider.read(id)).thenReturn(Optional.of(booking));
        });
    }

    private UpdateWorkshopSession updateWorkshopSession() {
        return updateWorkshopSession(clock);
    }

    private UpdateWorkshopSession updateWorkshopSession(Clock clock) {
        return new UpdateWorkshopSession(workshopVenueProvider, workshopSessionProvider, workshopBookingProvider,
                waitlist, mailProvider, clock, URI.create("https://eosa.me"));
    }

    private WorkshopSession scheduledSession() {
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(venueId);
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(8);
        session.setState(WorkshopSessionState.SCHEDULED);
        return session;
    }

    private WorkshopVenue venue(WorkshopVenueId id, boolean archived) {
        var venue = new WorkshopVenue();
        venue.setId(id);
        venue.setName("Bar du Coin");
        venue.setArchived(archived);
        return venue;
    }

}
