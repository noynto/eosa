package me.noynto.eosa.cart;

import java.math.BigDecimal;

public class CartShipping {
    private BigDecimal amount;
    private CartShippingRule rule;

    public CartShipping() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public CartShippingRule getRule() {
        return rule;
    }

    public void setRule(CartShippingRule rule) {
        this.rule = rule;
    }
}