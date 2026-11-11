package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mise en forme des commandes pour les templates de l'administration (Mustache n'a pas de logique) :
 * dates en heure de Paris, montants en euros, en français.
 */
final class OrderFormat {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy 'à' H 'h' mm", Locale.FRENCH);

    private OrderFormat() {
    }

    /** « 12 nov. 2026 à 10 h 15 » */
    static String dateTime(Instant instant) {
        return DATE_TIME.format(instant.atZone(BusinessTime.ZONE));
    }

    /** « 70,90 € » */
    static String amount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(amount);
    }

    static String state(OrderState state) {
        return switch (state) {
            case PAID -> "À fabriquer";
            case CRAFTED -> "Fabriquée";
            case SHIPPED -> "Expédiée";
            case DELIVERED -> "Livrée";
            case CANCELLED -> "Annulée";
        };
    }

}
