package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.GiftCardId;

import java.time.Clock;

/**
 * Annule une carte : elle passe en {@code VOID}, définitivement, et son solde restant est retiré du journal.
 * Seule une carte active s'annule.
 */
public record VoidGiftCard(
        GiftCardProvider giftCardProvider,
        Clock clock
) {

    private static final int MAX_REASON_LENGTH = 200;

    public Result handle(Command command) {
        if (command.giftCardId == null || command.giftCardId.value() == null) {
            throw new RuntimeException("L'identifiant de la carte est nécessaire.");
        }
        GiftCard giftCard = giftCardProvider.read(command.giftCardId)
                .orElseThrow(() -> new RuntimeException("La carte " + command.giftCardId.value() + " n'existe pas."));
        if (giftCard.getState() != GiftCardState.ACTIVE) {
            throw new RuntimeException("Seule une carte active peut être annulée.");
        }
        String reason = command.reason == null || command.reason.isBlank() ? null : command.reason.strip();
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new RuntimeException("Le motif est trop long.");
        }

        var remaining = giftCard.balance();
        giftCard.setState(GiftCardState.VOID);
        if (remaining.signum() > 0) {
            giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.VOIDED, remaining, reason, clock.instant()));
        }
        return new Result(giftCardProvider.write(giftCard));
    }

    public record Result(
            GiftCard giftCard
    ) {
    }

    public record Command(
            GiftCardId giftCardId,
            String reason
    ) {
    }

}
