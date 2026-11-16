package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import me.noynto.eosa.promo.PromoCodeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Lecture du formulaire d'un code de promotion. Un champ facultatif vide vaut {@code null} ; une saisie illisible
 * est refusée avec un message clair plutôt que d'être ignorée.
 */
record PromoCodeForm(
        PromoCodeType type,
        BigDecimal value,
        LocalDate startsOn,
        LocalDate endsOn,
        Integer maxUses,
        BigDecimal minimumAmount
) {

    static PromoCodeForm read(Context ctx) {
        return new PromoCodeForm(
                type(ctx.formParam("type")),
                decimal(ctx.formParam("value"), "La valeur de la remise"),
                date(ctx.formParam("startsOn"), "La date de début"),
                date(ctx.formParam("endsOn"), "La date de fin"),
                integer(ctx.formParam("maxUses"), "Le nombre d'usages"),
                decimal(ctx.formParam("minimumAmount"), "Le montant minimum")
        );
    }

    private static PromoCodeType type(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PromoCodeType.valueOf(value.strip());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Le type de remise est inconnu.");
        }
    }

    /** Accepte la virgule française : « 12,5 ». */
    private static BigDecimal decimal(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " doit être un nombre.");
        }
    }

    private static Integer integer(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " doit être un nombre entier.");
        }
    }

    private static LocalDate date(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new RuntimeException(label + " n'est pas une date valide.");
        }
    }

}
