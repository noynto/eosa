package me.noynto.eosa.application;

import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.util.List;
import java.util.Set;

public record ConfirmCheckoutSession(
        CheckoutProvider checkoutProvider,
        CartProvider cartProvider
) {

    public Checkout handle(Command command) throws Exception {
        if (command.checkoutSessionId == null || command.checkoutSessionId.getStripe().isBlank()) {
            throw new RuntimeException("L'identifiant de session d'achat est nécessaire.");
        }

        var checkoutSessionId = command.checkoutSessionId;

        // 1. Vérifie le statut du paiement chez Stripe
        Checkout checkout = this.checkoutProvider.readIds(checkoutSessionId)
                .findFirst()
                .flatMap(this.checkoutProvider::read)
                .orElseThrow(() -> new RuntimeException("Session Stripe introuvable."));

        if (Set.of(CheckoutStatus.PENDING, CheckoutStatus.CANCELLED, CheckoutStatus.EXPIRED).contains(checkout.getStatus())) {
            throw new RuntimeException("Le paiement n'a pas été confirmé.");
        }

        if (checkout.getCartId() == null) {
            throw new RuntimeException("Une commande doit forcément être issu d'un panier.");
        }

        cartProvider.read(checkout.getCartId())
                .ifPresent(cart -> {
                    cart.setItems(List.of());
                    cartProvider.write(cart);
                });

        return checkout;
    }

    public record Command(
            CheckoutSessionId checkoutSessionId
    ) {
    }

}