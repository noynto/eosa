package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.PromoDiscounts;
import me.noynto.eosa.shared.CartId;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Lit un panier existant, avec ses frais de livraison selon la règle en vigueur et la remise de son code de
 * promotion. La remise se recalcule à chaque lecture : un code qui ne s'applique plus (minimum non atteint,
 * usages épuisés) ne remise rien et dit pourquoi, sans être détaché du panier.
 */
public record ReadCart(
        CartProvider cartProvider,
        CartShippingRuleProvider shippingRuleProvider,
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));
        cart.applyShippingRule(shippingRuleProvider.get());

        if (cart.getPromoCodeId() == null || cart.getItems().isEmpty()) {
            return new Result(cart, null, BigDecimal.ZERO, null);
        }
        PromoCode promoCode = promoCodeProvider.read(cart.getPromoCodeId()).orElse(null);
        int uses = promoCode == null ? 0
                : (int) orderProvider.readIds(null, null, null, promoCode.getId()).count();
        var evaluation = PromoDiscounts.evaluate(promoCode, cart.getTotal(), cart.getShipping().getAmount(), uses, clock.instant());
        return new Result(cart, promoCode, evaluation.discount(), evaluation.refusal());
    }

    public record Command(
            CartId cartId
    ) {
    }

    /**
     * @param promoCode le code attaché au panier, null s'il n'y en a pas ou s'il a disparu
     * @param discount  remise accordée, zéro sans code ou quand il est refusé
     * @param refusal   pourquoi le code attaché ne remise rien, null quand il s'applique ou n'y en a pas
     */
    public record Result(
            Cart cart,
            PromoCode promoCode,
            BigDecimal discount,
            String refusal
    ) {
    }

}
