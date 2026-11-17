package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.service.BusinessTime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Libellés du tableau de bord : dates en heure de Paris, en français. */
final class DashboardFormat {

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);
    private static final DateTimeFormatter DAY_WITH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private DashboardFormat() {
    }

    /** « Mardi 18 novembre 2026 » */
    static String today(Instant now) {
        return capitalize(LONG_DATE.format(now.atZone(BusinessTime.ZONE)));
    }

    /** « jeudi 20 novembre » */
    static String day(Instant instant) {
        return DAY.format(instant.atZone(BusinessTime.ZONE));
    }

    /** « Samedi 22 novembre » */
    static String capitalizedDay(Instant instant) {
        return capitalize(day(instant));
    }

    /** « 20 nov. » */
    static String shortDay(Instant instant) {
        return SHORT_DAY.format(instant.atZone(BusinessTime.ZONE));
    }

    /** « 3 janv. 2027 » : le dernier jour de validité inclus, d'après une fin exclusive. L'année évite toute ambiguïté. */
    static String lastDayBefore(Instant exclusiveEnd) {
        return DAY_WITH_YEAR.format(exclusiveEnd.atZone(BusinessTime.ZONE).toLocalDate().minusDays(1));
    }

    /** « Novembre 2026 » */
    static String month(LocalDate firstDay) {
        return capitalize(MONTH.format(firstDay));
    }

    static String plural(int count, String singular, String plural) {
        return count > 1 ? plural : singular;
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

}
