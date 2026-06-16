package me.noynto.eosa.cart;

import java.math.BigDecimal;

public class CartShippingRule {
    private BigDecimal amount;
    private BigDecimal freeThreshold;

    public CartShippingRule() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFreeThreshold() {
        return freeThreshold;
    }

    public void setFreeThreshold(BigDecimal freeThreshold) {
        this.freeThreshold = freeThreshold;
    }
}
