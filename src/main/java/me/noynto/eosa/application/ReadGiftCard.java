package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.shared.GiftCardId;

/** Une carte cadeau avec son journal, pour l'administration. */
public record ReadGiftCard(
        GiftCardProvider giftCardProvider
) {

    public Result handle(Command command) {
        if (command.giftCardId == null || command.giftCardId.value() == null) {
            throw new RuntimeException("L'identifiant de la carte est nécessaire.");
        }
        return new Result(giftCardProvider.read(command.giftCardId)
                .orElseThrow(() -> new RuntimeException("La carte " + command.giftCardId.value() + " n'existe pas.")));
    }

    public record Result(
            GiftCard giftCard
    ) {
    }

    public record Command(
            GiftCardId giftCardId
    ) {
    }

}
