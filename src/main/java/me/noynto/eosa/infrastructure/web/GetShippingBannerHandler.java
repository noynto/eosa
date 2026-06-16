package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.cart.CartShippingRuleProvider;

import java.util.Map;

public record GetShippingBannerHandler(CartShippingRuleProvider shippingRuleProvider) implements Handler {

    @Override
    public void handle(Context ctx) {
        ctx.render("shipping-banner.jte", Map.of("rule", shippingRuleProvider.get()));
    }

}