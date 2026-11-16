package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.cart.SelectedCharm;
import me.noynto.eosa.checkout.*;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.GiftCardDeductions;
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
        GiftCardProvider giftCardProvider,
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

        // Une carte cadeau paie une part des bijoux : son solde est réservé avant d'ouvrir le paiement, pour que deux
        // paiements en même temps ne dépassent pas le solde. Sans code de promotion (jamais les deux).
        GiftCard giftCard = null;
        GiftCardMovement reservation = null;
        if (cart.getPromoCodeId() == null && cart.getGiftCardId() != null) {
            giftCard = giftCardProvider.read(cart.getGiftCardId()).orElse(null);
            var evaluation = GiftCardDeductions.evaluate(
                    giftCard, cart.getTotal(), cart.getShipping().getAmount(), clock.instant());
            if (evaluation.applies()) {
                reservation = giftCard.reserve(evaluation.deduction(), clock.instant());
                giftCard = giftCardProvider.write(giftCard);
                // Relue : si une autre réservation est passée entre-temps et que le solde ne suffit plus, la nôtre est
                // rendue et le paiement s'ouvre sans carte, comme l'a montré le panier.
                GiftCard fresh = giftCardProvider.read(giftCard.getId()).orElseThrow();
                if (fresh.balance().signum() < 0) {
                    String reservationId = reservation.getId().value();
                    fresh.getMovements().stream()
                            .filter(movement -> movement.getId() != null && reservationId.equals(movement.getId().value()))
                            .findFirst()
                            .ifPresent(own -> fresh.release(own, clock.instant()));
                    giftCardProvider.write(fresh);
                    reservation = null;
                } else {
                    giftCard = fresh;
                    String reservationId = reservation.getId().value();
                    reservation = fresh.getMovements().stream()
                            .filter(movement -> movement.getId() != null && reservationId.equals(movement.getId().value()))
                            .findFirst()
                            .orElseThrow();
                    checkout.setUsedGiftCardId(giftCard.getId());
                    checkout.setUsedGiftCardAmount(evaluation.deduction());
                    checkout.setExpiresAt(clock.instant().plus(PurchaseGiftCard.SESSION_DURATION));
                }
            }
        }

        Checkout opened;
        try {
            opened = checkoutProvider.write(checkout);
        } catch (RuntimeException e) {
            // Sans session de paiement, le solde mis de côté ne servirait à rien.
            if (reservation != null) {
                giftCard.release(reservation, clock.instant());
                giftCardProvider.write(giftCard);
            }
            throw e;
        }
        if (reservation != null) {
            // La réservation suit la session : à la confirmation, elle devient une utilisation ; sinon elle est rendue.
            reservation.setCheckoutSessionId(opened.getSession().getId().getStripe());
            giftCardProvider.write(giftCard);
        }
        return new Result(opened);
    }

    public record Result(
            Checkout checkout
    ) {
    }

    public record Command(CartId cartId) {
    }

}