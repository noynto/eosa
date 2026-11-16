package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.GiftCardDeductions;
import me.noynto.eosa.service.PromoDiscounts;
import me.noynto.eosa.shared.CartId;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Lit un panier existant, avec ses frais de livraison selon la règle en vigueur, la remise de son code de
 * promotion ou la part de sa carte cadeau (jamais les deux). Tout se recalcule à chaque lecture : un code ou une
 * carte qui ne s'applique plus (minimum non atteint, usages épuisés, solde épuisé) ne déduit rien et dit pourquoi,
 * sans être détaché du panier.
 */
public record ReadCart(
        CartProvider cartProvider,
        CartShippingRuleProvider shippingRuleProvider,
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider,
        GiftCardProvider giftCardProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));
        cart.applyShippingRule(shippingRuleProvider.get());

        if (cart.getItems().isEmpty() || (cart.getPromoCodeId() == null && cart.getGiftCardId() == null)) {
            return new Result(cart, null, BigDecimal.ZERO, null, null, BigDecimal.ZERO, null);
        }
        if (cart.getGiftCardId() != null) {
            GiftCard giftCard = giftCardProvider.read(cart.getGiftCardId()).orElse(null);
            var evaluation = GiftCardDeductions.evaluate(giftCard, cart.getTotal(), cart.getShipping().getAmount(), clock.instant());
            return new Result(cart, null, BigDecimal.ZERO, null, giftCard, evaluation.deduction(), evaluation.refusal());
        }
        PromoCode promoCode = promoCodeProvider.read(cart.getPromoCodeId()).orElse(null);
        int uses = promoCode == null ? 0
                : (int) orderProvider.readIds(null, null, null, promoCode.getId()).count();
        var evaluation = PromoDiscounts.evaluate(promoCode, cart.getTotal(), cart.getShipping().getAmount(), uses, clock.instant());
        return new Result(cart, promoCode, evaluation.discount(), evaluation.refusal(), null, BigDecimal.ZERO, null);
    }

    public record Command(
            CartId cartId
    ) {
    }

    /**
     * @param promoCode         le code attaché au panier, null s'il n'y en a pas ou s'il a disparu
     * @param discount          remise accordée, zéro sans code ou quand il est refusé
     * @param refusal           pourquoi le code attaché ne remise rien, null quand il s'applique ou n'y en a pas
     * @param giftCard          la carte attachée au panier, null s'il n'y en a pas ou si elle a disparu
     * @param giftCardDeduction part du panier payée par la carte, zéro sans carte ou quand elle est refusée
     * @param giftCardRefusal   pourquoi la carte attachée ne déduit rien, null quand elle s'applique ou n'y en a pas
     */
    public record Result(
            Cart cart,
            PromoCode promoCode,
            BigDecimal discount,
            String refusal,
            GiftCard giftCard,
            BigDecimal giftCardDeduction,
            String giftCardRefusal
    ) {
    }

}
