package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.PromoDiscounts;
import me.noynto.eosa.shared.CartId;

import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;

/**
 * Attache un code de promotion au panier. Le code est refusé, avec la raison, s'il ne s'applique pas au panier
 * tel qu'il est maintenant ; un seul code par panier, le nouveau remplace l'ancien.
 */
public record ApplyPromoCode(
        CartProvider cartProvider,
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider,
        CartShippingRuleProvider shippingRuleProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        // Majuscules, sans accent, sans espace : comme à la création du code.
        String code = command.code == null ? "" : Normalizer.normalize(command.code, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");
        if (code.isEmpty()) {
            throw new RuntimeException("Saisissez un code.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Votre panier est vide.");
        }
        if (cart.getGiftCardId() != null) {
            throw new RuntimeException("Retirez d'abord la carte cadeau pour utiliser un code de promotion.");
        }
        cart.applyShippingRule(shippingRuleProvider.get());

        PromoCode promoCode = promoCodeProvider.readIds(code).findFirst()
                .flatMap(promoCodeProvider::read)
                .orElse(null);
        int uses = promoCode == null ? 0
                : (int) orderProvider.readIds(null, null, null, promoCode.getId()).count();
        var evaluation = PromoDiscounts.evaluate(promoCode, cart.getTotal(), cart.getShipping().getAmount(), uses, clock.instant());
        if (!evaluation.applies()) {
            throw new RuntimeException(evaluation.refusal());
        }

        cart.setPromoCodeId(promoCode.getId());
        return new Result(cartProvider.write(cart), promoCode, evaluation.discount());
    }

    public record Result(
            Cart cart,
            PromoCode promoCode,
            java.math.BigDecimal discount
    ) {
    }

    public record Command(
            CartId cartId,
            String code
    ) {
    }

}
