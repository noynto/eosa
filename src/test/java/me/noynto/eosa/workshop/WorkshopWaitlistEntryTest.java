package me.noynto.eosa.workshop;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopWaitlistEntryTest {

    private final Instant now = Instant.parse("2026-11-21T12:00:00Z");

    @Test
    void canBeEmailed_whenNothingWasSentYet() {
        assertTrue(new WorkshopWaitlistEntry().canBeEmailed(now));
    }

    @Test
    void canBeEmailed_isFalseLessThanTheDelayAfterTheLastEmail() {
        var entry = new WorkshopWaitlistEntry();
        entry.setLastNotifiedAt(now.minus(WorkshopWaitlistEntry.MIN_DELAY_BETWEEN_EMAILS).plusSeconds(1));

        assertFalse(entry.canBeEmailed(now));
    }

    @Test
    void canBeEmailed_isTrueExactlyAtTheDelayAndAfter() {
        var entry = new WorkshopWaitlistEntry();
        entry.setLastNotifiedAt(now.minus(WorkshopWaitlistEntry.MIN_DELAY_BETWEEN_EMAILS));
        assertTrue(entry.canBeEmailed(now));

        entry.setLastNotifiedAt(now.minus(Duration.ofDays(2)));
        assertTrue(entry.canBeEmailed(now));
    }

}
