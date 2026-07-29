package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.GetOrCreateCart;
import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartShipping;
import me.noynto.eosa.shared.CartId;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public record GetCartHandler(GetOrCreateCart getOrCreateCart) implements Handler {

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
            Map<String, Object> line = new HashMap<>();
            line.put("productId", item.productId().value());
            line.put("hasImage", hasImage);
            line.put("imageId", hasImage ? item.imageId().value() : "");
            line.put("name", item.name());
            line.put("quantity", item.quantity());
            line.put("decrementedQuantity", item.quantity() - 1);
            line.put("incrementedQuantity", item.quantity() + 1);
            line.put("lineTotal", item.price().multiply(BigDecimal.valueOf(item.quantity())).stripTrailingZeros().toPlainString());
            return line;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Votre panier");
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