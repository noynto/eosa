package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaveWorkshopWaitlistTest {

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final InMemoryWorkshopWaitlistEntries waitlist = new InMemoryWorkshopWaitlistEntries();

    @Test
    void handle_removesTheEntryOnlyOnce() {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(new WorkshopSessionId("session"));
        entry.setToken("token");
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        waitlist.write(entry);

        new LeaveWorkshopWaitlist(waitlist, Clock.fixed(now, ZoneOffset.UTC)).handle(new LeaveWorkshopWaitlist.Command("token"));
        new LeaveWorkshopWaitlist(waitlist, Clock.fixed(now.plus(Duration.ofHours(1)), ZoneOffset.UTC))
                .handle(new LeaveWorkshopWaitlist.Command("token"));

        assertEquals(now, entry.getRemovedAt());
    }

    @Test
    void handle_throwsForAnUnknownToken() {
        assertThrows(RuntimeException.class, () ->
                new LeaveWorkshopWaitlist(waitlist, Clock.fixed(now, ZoneOffset.UTC)).handle(new LeaveWorkshopWaitlist.Command("other")));
    }

}
