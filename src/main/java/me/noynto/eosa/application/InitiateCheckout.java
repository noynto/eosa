package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.cart.SelectedCharm;
import me.noynto.eosa.checkout.*;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.PromoDiscounts;
import me.noynto.eosa.shared.CartId;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ouvre le paiement du panier. La remise du code de promotion est recalculée ici, sur le panier tel qu'il est : un
 * code qui ne s'applique plus ne remise rien, comme l'a montré le panier, et jamais plus que ce qu'il permet.
 */
public record InitiateCheckout(
        CartProvider cartProvider,
        CheckoutProvider checkoutProvider,
        CartShippingRuleProvider shippingRuleProvider,
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle(Command command) throws Exception {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide.");
        }

        cart.applyShippingRule(shippingRuleProvider.get());

        List<CheckoutItem> checkoutItems = cart.getItems()
                .stream()
                .map(item -> {
                    StringBuilder name = new StringBuilder(item.name());
                    if (item.metalColorName() != null) {
                        name.append(" — ").append(item.metalColorName());
                    }
                    if (!item.charms().isEmpty()) {
                        name.append(" (+ ").append(item.charms().stream()
                                .map(SelectedCharm::name)
                                .collect(Collectors.joining(", "))).append(")");
                    }
                    CheckoutItem checkoutItem = new CheckoutItem();
                    checkoutItem.setName(name.toString());
                    checkoutItem.setUnitPrice(item.effectiveUnitPrice());
                    checkoutItem.setQuantity(item.quantity());
                    checkoutItem.setJewelId(item.jewelId());
                    checkoutItem.setImageId(item.imageId());
                    return checkoutItem;
                })
                .toList();

        Checkout checkout = new Checkout();
        checkout.setCartId(command.cartId);
        checkout.setItems(checkoutItems);
        checkout.setShippingAmount(cart.getShipping().getAmount());
        if (cart.getPromoCodeId() != null) {
            PromoCode promoCode = promoCodeProvider.read(cart.getPromoCodeId()).orElse(null);
            int uses = promoCode == null ? 0
                    : (int) orderProvider.readIds(null, null, null, promoCode.getId()).count();
            var evaluation = PromoDiscounts.evaluate(promoCode, cart.getTotal(), cart.getShipping().getAmount(), uses, clock.instant());
            if (evaluation.applies()) {
                checkout.setDiscountAmount(evaluation.discount());
                checkout.setPromoCodeId(promoCode.getId());
                checkout.setPromoCode(promoCode.getCode());
            }
        }
        checkout.setStatus(CheckoutStatus.PENDING);
        return new Result(checkoutProvider.write(checkout));
    }

    public record Result(
            Checkout checkout
    ) {
    }

    public record Command(CartId cartId) {
    }

}