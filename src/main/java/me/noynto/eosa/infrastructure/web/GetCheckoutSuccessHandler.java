package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ConfirmCheckoutSession;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetCheckoutSuccessHandler(
        ConfirmCheckoutSession confirmCheckoutSession
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        String stripeSessionId = ctx.queryParam("session_id");
        CheckoutSessionId checkoutSessionId = new CheckoutSessionId();
        checkoutSessionId.setStripe(stripeSessionId);
        String cartCookie = ctx.cookie("cart");
        var session = confirmCheckoutSession.handle(new ConfirmCheckoutSession.Command(checkoutSessionId));
        ctx.cookie("cart", "");

        boolean isCompleted = session.getStatus() == CheckoutStatus.COMPLETED;
        boolean isExpired = session.getStatus() == CheckoutStatus.EXPIRED;
        boolean hasItems = session.getItems() != null && !session.getItems().isEmpty();
        List<Map<String, Object>> items = hasItems ? session.getItems().stream().map(item -> {
            Map<String, Object> line = new HashMap<>();
            line.put("name", item.getName());
            line.put("quantity", item.getQuantity());
            line.put("lineTotal", item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())).stripTrailingZeros().toPlainString());
            return line;
        }).toList() : List.of();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Commande confirmée");
        model.put("isCompleted", isCompleted);
        model.put("isExpired", isExpired);
        model.put("hasItems", hasItems);
        model.put("items", items);
        ctx.render("checkout/success.mustache", model);
    }

}