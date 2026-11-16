package me.noynto.eosa.cart;

import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.PromoCodeId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Cart {
    private CartId id;
    private List<CartItem> items = new ArrayList<>();
    private CartShipping shipping;
    private PromoCodeId promoCodeId;

    /**
     * Le code de promotion que la cliente a saisi, ou null. Il reste attaché même quand il ne s'applique plus
     * (minimum non atteint) : la remise se recalcule à chaque lecture.
     */
    public PromoCodeId getPromoCodeId() {
        return promoCodeId;
    }

    public void setPromoCodeId(PromoCodeId promoCodeId) {
        this.promoCodeId = promoCodeId;
    }

    public CartId getId() {
        return id;
    }

    public void setId(CartId id) {
        this.id = id;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public CartShipping getShipping() {
        return shipping;
    }

    public void setShipping(CartShipping shipping) {
        this.shipping = shipping;
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(i -> i.effectiveUnitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void applyShippingRule(CartShippingRule rule) {
        CartShipping cartShipping = new CartShipping();
        cartShipping.setAmount(getTotal().compareTo(rule.getFreeThreshold()) < 0 ? rule.getAmount() : BigDecimal.ZERO);
        cartShipping.setRule(rule);
        this.shipping = cartShipping;
    }
}