package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.cart.CartShippingRuleProvider;

import java.util.HashMap;
import java.util.Map;

public record GetShippingPageHandler(CartShippingRuleProvider shippingRuleProvider, String baseUrl) implements Handler {

    @Override
    public void handle(Context ctx) {
        var rule = shippingRuleProvider.get();
        String freeThreshold = rule.getFreeThreshold().stripTrailingZeros().toPlainString();
        String shippingAmount = rule.getAmount().stripTrailingZeros().toPlainString();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Livraison");
        model.put("description", "Livraison en France via Mondial Relay, offerte dès " + freeThreshold + " €.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + ctx.path());
        model.put("freeThreshold", freeThreshold);
        model.put("shippingAmount", shippingAmount);
        ctx.render("shipping.mustache", model);
    }

}
