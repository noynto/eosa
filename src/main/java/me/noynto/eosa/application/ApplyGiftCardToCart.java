package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.service.GiftCardDeductions;
import me.noynto.eosa.shared.CartId;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;

/**
 * Attache une carte cadeau au panier. Refusée, avec la raison, si elle ne s'applique pas au panier tel qu'il est
 * maintenant, ou si le panier porte déjà un code de promotion (jamais les deux). Une seule carte par panier, la
 * nouvelle remplace l'ancienne.
 */
public record ApplyGiftCardToCart(
        CartProvider cartProvider,
        GiftCardProvider giftCardProvider,
        CartShippingRuleProvider shippingRuleProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        // Majuscules, sans accent, sans espace ni tiret : la saisie d'un code lu sur une carte imprimée.
        String code = command.code == null ? "" : Normalizer.normalize(command.code, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "");
        if (code.isEmpty()) {
            throw new RuntimeException("Saisissez un code.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Votre panier est vide.");
        }
        if (cart.getPromoCodeId() != null) {
            throw new RuntimeException("Retirez d'abord le code de promotion pour utiliser une carte cadeau.");
        }
        cart.applyShippingRule(shippingRuleProvider.get());

        GiftCard giftCard = giftCardProvider.readIds(code, null).findFirst()
                .flatMap(giftCardProvider::read)
                .orElse(null);
        var evaluation = GiftCardDeductions.evaluate(giftCard, cart.getTotal(), cart.getShipping().getAmount(), clock.instant());
        if (!evaluation.applies()) {
            throw new RuntimeException(evaluation.refusal());
        }

        cart.setGiftCardId(giftCard.getId());
        return new Result(cartProvider.write(cart), giftCard, evaluation.deduction());
    }

    public record Result(
            Cart cart,
            GiftCard giftCard,
            BigDecimal deduction
    ) {
    }

    public record Command(
            CartId cartId,
            String code
    ) {
    }

}
