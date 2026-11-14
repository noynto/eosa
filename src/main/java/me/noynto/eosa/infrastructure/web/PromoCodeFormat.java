package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.service.BusinessTime;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Libellés des codes de promotion pour l'administration. */
final class PromoCodeFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    private PromoCodeFormat() {
    }

    /** « 10 % » ou « 15,00 € ». */
    static String discount(PromoCode promoCode) {
        return promoCode.getType() == PromoCodeType.PERCENT
                ? promoCode.getValue().stripTrailingZeros().toPlainString() + " %"
                : OrderFormat.amount(promoCode.getValue());
    }

    /** Valeur à remettre dans le champ du formulaire : « 10 » ou « 15.00 ». */
    static String inputValue(PromoCode promoCode) {
        return promoCode.getType() == PromoCodeType.PERCENT
                ? promoCode.getValue().stripTrailingZeros().toPlainString()
                : promoCode.getValue().toPlainString();
    }

    /** Premier jour de validité, au format d'un champ de date, ou vide. */
    static String startsOn(PromoCode promoCode) {
        return promoCode.getStartsAt() == null ? "" : promoCode.getStartsAt().atZone(BusinessTime.ZONE).toLocalDate().toString();
    }

    /** Dernier jour de validité inclus (la fin enregistrée est exclusive), ou vide. */
    static String endsOn(PromoCode promoCode) {
        return promoCode.getEndsAt() == null ? ""
                : promoCode.getEndsAt().atZone(BusinessTime.ZONE).toLocalDate().minusDays(1).toString();
    }

    /** « Du 1 nov. 2026 au 30 nov. 2026 », « Jusqu'au … », « À partir du … » ou « Sans limite de date ». */
    static String validity(PromoCode promoCode) {
        String from = promoCode.getStartsAt() == null ? null
                : DATE.format(promoCode.getStartsAt().atZone(BusinessTime.ZONE));
        String to = promoCode.getEndsAt() == null ? null
                : DATE.format(promoCode.getEndsAt().atZone(BusinessTime.ZONE).toLocalDate().minusDays(1));
        if (from != null && to != null) {
            return "Du " + from + " au " + to;
        }
        if (to != null) {
            return "Jusqu'au " + to;
        }
        if (from != null) {
            return "À partir du " + from;
        }
        return "Sans limite de date";
    }

    /** Ce que la cliente verrait si elle saisissait le code maintenant. */
    static String status(PromoCode promoCode, Instant now) {
        if (!promoCode.isActive()) {
            return "Désactivé";
        }
        if (promoCode.getEndsAt() != null && !now.isBefore(promoCode.getEndsAt())) {
            return "Expiré";
        }
        if (promoCode.getStartsAt() != null && now.isBefore(promoCode.getStartsAt())) {
            return "Pas encore valide";
        }
        return "Actif";
    }

}
