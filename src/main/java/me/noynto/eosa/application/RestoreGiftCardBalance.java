package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.GiftCardId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;

/**
 * Recrédite une carte à la main, par exemple après l'annulation d'une commande payée en partie par cette carte.
 * Le motif est obligatoire : il s'écrit dans le journal. Une carte annulée ou expirée ne se réactive pas.
 */
public record RestoreGiftCardBalance(
        GiftCardProvider giftCardProvider,
        Clock clock
) {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000");
    private static final int MAX_REASON_LENGTH = 200;

    public Result handle(Command command) {
        if (command.giftCardId == null || command.giftCardId.value() == null) {
            throw new RuntimeException("L'identifiant de la carte est nécessaire.");
        }
        if (command.amount == null || command.amount.signum() <= 0 || command.amount.compareTo(MAX_AMOUNT) > 0) {
            throw new RuntimeException("Le montant doit être positif et d'au plus 1 000 €.");
        }
        String reason = command.reason == null || command.reason.isBlank() ? null : command.reason.strip();
        if (reason == null || reason.length() > MAX_REASON_LENGTH) {
            throw new RuntimeException("Le motif du recrédit est requis.");
        }
        GiftCard giftCard = giftCardProvider.read(command.giftCardId)
                .orElseThrow(() -> new RuntimeException("La carte " + command.giftCardId.value() + " n'existe pas."));
        var now = clock.instant();
        if (giftCard.getState() != GiftCardState.ACTIVE) {
            throw new RuntimeException("Seule une carte active peut être recréditée.");
        }
        if (giftCard.isExpired(now)) {
            throw new RuntimeException("Cette carte est expirée : elle ne peut plus être recréditée.");
        }

        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.RESTORED,
                command.amount.setScale(2, RoundingMode.HALF_UP), reason, now));
        return new Result(giftCardProvider.write(giftCard));
    }

    public record Result(
            GiftCard giftCard
    ) {
    }

    public record Command(
            GiftCardId giftCardId,
            BigDecimal amount,
            String reason
    ) {
    }

}
