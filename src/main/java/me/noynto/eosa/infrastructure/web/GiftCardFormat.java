package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.service.BusinessTime;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Libellés des cartes cadeau pour l'administration. */
final class GiftCardFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    private GiftCardFormat() {
    }

    /** Le code sans ses 4 derniers caractères en clair : « ••••••••AB3K ». */
    static String maskedCode(GiftCard giftCard) {
        String code = giftCard.getCode();
        return "••••••••" + code.substring(Math.max(0, code.length() - 4));
    }

    static String status(GiftCard giftCard, Instant now) {
        if (giftCard.getState() == GiftCardState.PENDING_PAYMENT) {
            return "En attente du paiement";
        }
        if (giftCard.getState() == GiftCardState.VOID) {
            return "Annulée";
        }
        if (giftCard.isExpired(now)) {
            return "Expirée";
        }
        return giftCard.balance().signum() > 0 ? "Active" : "Soldée";
    }

    /** Dernier jour de validité inclus (la fin enregistrée est exclusive). */
    static String validUntil(GiftCard giftCard) {
        return DATE.format(giftCard.getExpiresAt().atZone(BusinessTime.ZONE).toLocalDate().minusDays(1));
    }

    static String movementKind(GiftCardMovementKind kind) {
        return switch (kind) {
            case ISSUED -> "Émission";
            case RESERVED -> "Réservé";
            case USED -> "Utilisé";
            case RELEASED -> "Libéré";
            case VOIDED -> "Annulation";
            case RESTORED -> "Recrédité";
        };
    }

    /** Le signe affiché devant le montant d'un mouvement : « + », « − » ou rien. */
    static String movementSign(GiftCardMovementKind kind) {
        return switch (kind) {
            case ISSUED, RELEASED, RESTORED -> "+";
            case RESERVED, VOIDED -> "−";
            case USED -> "";
        };
    }

}
