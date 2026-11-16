package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;

import java.time.Clock;

/**
 * La page imprimable d'une carte, ouverte par son jeton. Une carte en attente de paiement n'existe pas encore
 * pour le public : elle se lit comme une carte inconnue.
 */
public record ReadGiftCardByToken(
        GiftCardProvider giftCardProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.token == null || command.token.isBlank()) {
            throw new RuntimeException("Carte cadeau introuvable.");
        }
        GiftCard giftCard = giftCardProvider.readIds(null, command.token)
                .findFirst()
                .flatMap(giftCardProvider::read)
                .filter(card -> card.getState() != GiftCardState.PENDING_PAYMENT)
                .orElseThrow(() -> new RuntimeException("Carte cadeau introuvable."));
        return new Result(giftCard, giftCard.isUsable(clock.instant()));
    }

    public record Result(
            GiftCard giftCard,
            boolean usable
    ) {
    }

    public record Command(
            String token
    ) {
    }

}
