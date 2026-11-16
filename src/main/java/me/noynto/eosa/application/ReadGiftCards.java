package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;

import java.util.List;

/** Les cartes cadeau, les plus récentes d'abord, avec leur journal (le solde en est la somme). */
public record ReadGiftCards(
        GiftCardProvider giftCardProvider
) {

    public Result handle() {
        return new Result(giftCardProvider.readIds(null, null)
                .map(id -> giftCardProvider.read(id).orElseThrow())
                .toList());
    }

    public record Result(
            List<GiftCard> giftCards
    ) {
    }

}
