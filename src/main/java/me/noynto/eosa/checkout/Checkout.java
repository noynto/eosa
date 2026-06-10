package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CheckoutId;

import java.util.List;

public class Checkout {
    private CheckoutId id;
    private CheckoutSession session;
    private List<CheckoutItem> items;
    private CheckoutStatus status;
    private CartId cartId;

    public Checkout() {
    }

    public CheckoutId getId() {
        return id;
    }

    public void setId(CheckoutId id) {
        this.id = id;
    }

    public List<CheckoutItem> getItems() {
        return items;
    }

    public void setItems(List<CheckoutItem> items) {
        this.items = items;
    }

    public CheckoutStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutStatus status) {
        this.status = status;
    }

    public CheckoutSession getSession() {
        return session;
    }

    public void setSession(CheckoutSession session) {
        this.session = session;
    }

    public CartId getCartId() {
        return cartId;
    }

    public void setCartId(CartId cartId) {
        this.cartId = cartId;
    }
}
