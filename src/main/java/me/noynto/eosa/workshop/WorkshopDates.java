package me.noynto.eosa.workshop;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Dates des ateliers telles qu'on les affiche (pages et e-mails) : en français, en heure de Paris.
 */
public final class WorkshopDates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H 'h' mm", Locale.FRENCH);

    private WorkshopDates() {
    }

    /** « Samedi 14 novembre 2026 » */
    public static String date(Instant instant) {
        String date = DATE.format(instant.atZone(WorkshopSession.ZONE));
        return Character.toUpperCase(date.charAt(0)) + date.substring(1);
    }

    /** « samedi 14 novembre 2026 », au fil d'une phrase. */
    public static String dateInSentence(Instant instant) {
        return DATE.format(instant.atZone(WorkshopSession.ZONE));
    }

    /** « 14 h 00 » */
    public static String time(Instant instant) {
        return TIME.format(instant.atZone(WorkshopSession.ZONE));
    }

    /** « 14 h 00 – 16 h 00 » */
    public static String timeRange(WorkshopSession session) {
        return time(session.getStartsAt()) + " – " + time(session.getEndsAt());
    }

    /** « 2 h », « 1 h 30 », « 45 min » */
    public static String duration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours == 0) {
            return rest + " min";
        }
        return rest == 0 ? hours + " h" : hours + " h " + String.format("%02d", rest);
    }

}
