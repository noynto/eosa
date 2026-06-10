package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.checkout.*;
import me.noynto.eosa.shared.CartId;

import java.util.List;

public record InitiateCheckout(
        CartProvider cartProvider,
        CheckoutProvider checkoutProvider
) {

    public Checkout handle(Command command) throws Exception {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide.");
        }

        List<CheckoutItem> checkoutItems = cart.getItems()
                .stream()
                .map(item -> {
                    CheckoutItem checkoutItem = new CheckoutItem();
                    checkoutItem.setName(item.name());
                    checkoutItem.setUnitPrice(item.price());
                    checkoutItem.setQuantity(item.quantity());
                    checkoutItem.setProductId(item.productId());
                    checkoutItem.setImageId(item.imageId());
                    return checkoutItem;
                })
                .toList();

        Checkout checkout = new Checkout();
        checkout.setCartId(command.cartId);
        checkout.setItems(checkoutItems);
        checkout.setStatus(CheckoutStatus.PENDING);
        return checkoutProvider.write(checkout);
    }

    public record Command(CartId cartId) {
    }

}