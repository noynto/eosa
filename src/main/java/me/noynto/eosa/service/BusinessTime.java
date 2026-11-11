package me.noynto.eosa.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Délais comptés en heures ouvrées : la durée ne s'écoule que les jours ouvrés (du lundi au vendredi, hors jours
 * fériés français), 24 h par jour ouvré, dans le fuseau {@code Europe/Paris}. Sans provider, l'heure est toujours
 * un paramètre.
 */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private BusinessTime() {
    }

    /**
     * Échéance obtenue en ajoutant {@code hours} heures ouvrées à {@code start}.
     */
    public static Instant deadline(Instant start, long hours) {
        ZonedDateTime cursor = start.atZone(ZONE);
        long remaining = Duration.ofHours(hours).toSeconds();
        while (true) {
            if (!isBusinessDay(cursor.toLocalDate())) {
                cursor = cursor.toLocalDate().plusDays(1).atStartOfDay(ZONE);
                continue;
            }
            ZonedDateTime endOfDay = cursor.toLocalDate().plusDays(1).atStartOfDay(ZONE);
            long available = Duration.between(cursor, endOfDay).toSeconds();
            if (remaining <= available) {
                return cursor.plusSeconds(remaining).toInstant();
            }
            remaining -= available;
            cursor = endOfDay;
        }
    }

    /**
     * Vrai si l'échéance obtenue en ajoutant {@code hours} heures ouvrées à {@code start} est dépassée à {@code now}.
     */
    public static boolean isOverdue(Instant start, long hours, Instant now) {
        return now.isAfter(deadline(start, hours));
    }

    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !publicHolidays(date.getYear()).contains(date);
    }

    /**
     * Jours fériés français d'une année, dont ceux qui dépendent de Pâques.
     */
    public static Set<LocalDate> publicHolidays(int year) {
        LocalDate easter = easter(year);
        return Set.of(
                LocalDate.of(year, 1, 1),
                easter.plusDays(1),
                LocalDate.of(year, 5, 1),
                LocalDate.of(year, 5, 8),
                easter.plusDays(39),
                easter.plusDays(50),
                LocalDate.of(year, 7, 14),
                LocalDate.of(year, 8, 15),
                LocalDate.of(year, 11, 1),
                LocalDate.of(year, 11, 11),
                LocalDate.of(year, 12, 25)
        );
    }

    // Algorithme de Meeus / Jones / Butcher.
    private static LocalDate easter(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }

}
