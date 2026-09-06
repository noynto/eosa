package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.GetOrCreateCart;
import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartShipping;
import me.noynto.eosa.shared.CartId;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetCartHandler(GetOrCreateCart getOrCreateCart, String baseUrl) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        Cart cart = getOrCreateCart.handle(new GetOrCreateCart.Command(
                cookieValue != null ? new CartId(cookieValue) : null
        ));
        ctx.cookie("cart", cart.getId().value());

        CartShipping shipping = cart.getShipping();
        BigDecimal subtotal = cart.getTotal();
        BigDecimal shippingAmount = shipping != null ? shipping.getAmount() : null;
        BigDecimal total = shippingAmount != null ? subtotal.add(shippingAmount) : subtotal;
        BigDecimal remaining = shipping != null ? shipping.getRule().getFreeThreshold().subtract(subtotal) : BigDecimal.ZERO;

        boolean hasItems = !cart.getItems().isEmpty();
        boolean isShippingPaid = shippingAmount != null && shippingAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasRemainingForFreeShipping = shipping != null && remaining.compareTo(BigDecimal.ZERO) > 0;

        var items = cart.getItems().stream().map(item -> {
            boolean hasImage = item.imageId() != null;
            boolean hasMetalColor = item.metalColorName() != null;
            boolean hasMetalColorImage = item.metalColorImageId() != null;
            Map<String, Object> line = new HashMap<>();
            line.put("id", item.id().value());
            line.put("jewelId", item.jewelId().value());
            line.put("hasImage", hasImage);
            line.put("imageId", hasImage ? item.imageId().value() : "");
            line.put("name", item.name());
            line.put("hasMetalColor", hasMetalColor);
            line.put("metalColorName", hasMetalColor ? item.metalColorName() : "");
            line.put("hasMetalColorImage", hasMetalColorImage);
            line.put("metalColorImageId", hasMetalColorImage ? item.metalColorImageId().value() : "");
            line.put("quantity", item.quantity());
            line.put("decrementedQuantity", item.quantity() - 1);
            line.put("incrementedQuantity", item.quantity() + 1);
            line.put("lineTotal", item.effectiveUnitPrice().multiply(BigDecimal.valueOf(item.quantity())).stripTrailingZeros().toPlainString());
            List<Map<String, Object>> charms = item.charms().stream().map(charm -> {
                boolean hasCharmImage = charm.imageId() != null;
                Map<String, Object> charmLine = new HashMap<>();
                charmLine.put("name", charm.name());
                charmLine.put("price", charm.price().stripTrailingZeros().toPlainString());
                charmLine.put("hasImage", hasCharmImage);
                charmLine.put("imageId", hasCharmImage ? charm.imageId().value() : "");
                return charmLine;
            }).toList();
            line.put("hasCharms", !charms.isEmpty());
            line.put("charms", charms);
            return line;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Votre panier");
        model.put("description", "Votre panier Eosa — bijoux faits main à Nancy.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + ctx.path());
        model.put("itemCountLabel", cart.getItems().size() + " article" + (cart.getItems().size() > 1 ? "s" : ""));
        model.put("hasItems", hasItems);
        model.put("items", items);
        model.put("subtotal", subtotal.stripTrailingZeros().toPlainString());
        model.put("isShippingPaid", isShippingPaid);
        model.put("shippingAmount", isShippingPaid ? shippingAmount.stripTrailingZeros().toPlainString() : "");
        model.put("total", total.stripTrailingZeros().toPlainString());
        model.put("hasRemainingForFreeShipping", hasRemainingForFreeShipping);
        model.put("remaining", hasRemainingForFreeShipping ? remaining.stripTrailingZeros().toPlainString() : "");
        ctx.render("cart.mustache", model);
    }

}