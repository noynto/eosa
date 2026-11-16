package me.noynto.eosa.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessTimeTest {

    private static Instant paris(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(BusinessTime.ZONE).toInstant();
    }

    @Test
    void countsTwentyFourHoursPerBusinessDay() {
        // Lundi 16 novembre 2026, 10 h + 48 h = mercredi 18, 10 h.
        assertEquals(paris(2026, 11, 18, 10, 0), BusinessTime.deadline(paris(2026, 11, 16, 10, 0), 48));
    }

    @Test
    void skipsTheWeekend() {
        // Vendredi 13 novembre 2026, 20 h : 4 h le vendredi, rien le week-end, 24 h le lundi, 20 h le mardi.
        assertEquals(paris(2026, 11, 17, 20, 0), BusinessTime.deadline(paris(2026, 11, 13, 20, 0), 48));
    }

    @Test
    void startsOnTheNextBusinessDayWhenPaidOnAWeekend() {
        // Samedi 14 novembre 2026, 15 h : le délai court à partir de lundi 0 h.
        assertEquals(paris(2026, 11, 18, 0, 0), BusinessTime.deadline(paris(2026, 11, 14, 15, 0), 48));
    }

    @Test
    void skipsFrenchPublicHolidays() {
        // Mardi 10 novembre 2026, 12 h + 24 h : le 11 novembre est férié, donc jeudi 12, 12 h.
        assertEquals(paris(2026, 11, 12, 12, 0), BusinessTime.deadline(paris(2026, 11, 10, 12, 0), 24));
    }

    @Test
    void knowsTheHolidaysThatDependOnEaster() {
        // Pâques 2026 : dimanche 5 avril.
        assertTrue(BusinessTime.publicHolidays(2026).contains(LocalDate.of(2026, 4, 6)));
        assertTrue(BusinessTime.publicHolidays(2026).contains(LocalDate.of(2026, 5, 14)));
        assertTrue(BusinessTime.publicHolidays(2026).contains(LocalDate.of(2026, 5, 25)));
        // Pâques 2027 : dimanche 28 mars.
        assertTrue(BusinessTime.publicHolidays(2027).contains(LocalDate.of(2027, 3, 29)));
    }

    @Test
    void tellsWhetherADeadlineIsPassed() {
        var start = paris(2026, 11, 16, 10, 0);
        assertFalse(BusinessTime.isOverdue(start, 48, paris(2026, 11, 18, 10, 0)));
        assertTrue(BusinessTime.isOverdue(start, 48, paris(2026, 11, 18, 10, 1)));
    }

}
