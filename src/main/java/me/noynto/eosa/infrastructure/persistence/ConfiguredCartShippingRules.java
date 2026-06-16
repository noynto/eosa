package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;

import java.math.BigDecimal;
import java.util.Objects;

public class ConfiguredCartShippingRules implements CartShippingRuleProvider {

    private static final String AMOUNT = "EOSA_SHIPPING_AMOUNT";
    private static final String FREE_THRESHOLD = "EOSA_SHIPPING_FREE_THRESHOLD";

    @Override
    public CartShippingRule get() {
        CartShippingRule rule = new CartShippingRule();
        rule.setAmount(new BigDecimal(Objects.requireNonNull(System.getenv(AMOUNT), "Le montant des frais de livraison est obligatoire.")));
        rule.setFreeThreshold(new BigDecimal(Objects.requireNonNull(System.getenv(FREE_THRESHOLD), "Le seuil de livraison gratuite est obligatoire.")));
        return rule;
    }

}