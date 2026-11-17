package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCart;
import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartShipping;
import me.noynto.eosa.shared.CartId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetCartHandler(ReadCart readCart, String baseUrl) implements Handler {

    @Override
    public void handle(Context ctx) {
        // Posé par EnsureCartHandler, qui a déjà créé le panier au besoin.
        CartId cartId = ctx.attribute("cartId");
        var result = readCart.handle(new ReadCart.Command(cartId));
        Cart cart = result.cart();

        CartShipping shipping = cart.getShipping();
        BigDecimal subtotal = cart.getTotal();
        BigDecimal discount = result.discount();
        BigDecimal shippingAmount = shipping != null ? shipping.getAmount() : null;
        BigDecimal giftCardDeduction = result.giftCardDeduction();
        BigDecimal total = (shippingAmount != null ? subtotal.add(shippingAmount) : subtotal)
                .subtract(discount).subtract(giftCardDeduction);
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
            line.put("lineTotal", price(item.effectiveUnitPrice().multiply(BigDecimal.valueOf(item.quantity()))));
            List<Map<String, Object>> charms = item.charms().stream().map(charm -> {
                boolean hasCharmImage = charm.imageId() != null;
                Map<String, Object> charmLine = new HashMap<>();
                charmLine.put("name", charm.name());
                charmLine.put("price", price(charm.price()));
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
        model.put("subtotal", price(subtotal));
        // Le code saisi, qu'il remise ou non : refusé, il reste affiché avec la raison et peut être retiré.
        boolean hasPromoCode = result.promoCode() != null;
        boolean hasDiscount = discount.signum() > 0;
        model.put("hasPromoCode", hasPromoCode);
        model.put("promoCode", hasPromoCode ? result.promoCode().getCode() : "");
        model.put("hasDiscount", hasDiscount);
        model.put("discount", hasDiscount ? price(discount) : "");
        model.put("hasPromoRefusal", hasPromoCode && result.refusal() != null);
        model.put("promoRefusal", result.refusal() != null ? result.refusal() : "");
        // Une carte cadeau, comme un code de promotion : saisie ou refusée, elle reste affichée avec la raison.
        boolean hasGiftCard = result.giftCard() != null;
        boolean hasGiftCardDeduction = giftCardDeduction.signum() > 0;
        model.put("hasGiftCard", hasGiftCard);
        model.put("giftCardCode", hasGiftCard ? maskedCode(result.giftCard().getCode()) : "");
        model.put("hasGiftCardDeduction", hasGiftCardDeduction);
        model.put("giftCardDeduction", hasGiftCardDeduction ? price(giftCardDeduction) : "");
        model.put("hasGiftCardRefusal", hasGiftCard && result.giftCardRefusal() != null);
        model.put("giftCardRefusal", result.giftCardRefusal() != null ? result.giftCardRefusal() : "");
        BigDecimal giftCardRemaining = hasGiftCardDeduction ? result.giftCard().balance().subtract(giftCardDeduction) : BigDecimal.ZERO;
        model.put("hasGiftCardRemaining", hasGiftCardDeduction && giftCardRemaining.signum() > 0);
        model.put("giftCardRemaining", price(giftCardRemaining));
        // Un code de promotion ou une carte cadeau, jamais les deux : on saisit l'un tant que l'autre est absent.
        model.put("canEnterPromoCode", hasItems && !hasPromoCode && !hasGiftCard);
        model.put("canEnterGiftCard", hasItems && !hasPromoCode && !hasGiftCard);
        model.put("isShippingPaid", isShippingPaid);
        model.put("shippingAmount", isShippingPaid ? price(shippingAmount) : "");
        model.put("total", price(total));
        model.put("hasRemainingForFreeShipping", hasRemainingForFreeShipping);
        model.put("remaining", hasRemainingForFreeShipping ? price(remaining) : "");
        ctx.render("cart.mustache", model);
    }

    /** « 42 » pour un montant rond, « 4,90 » sinon : la virgule du français, deux décimales. */
    static String price(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return stripped.toPlainString();
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Le code d'une carte saisie dans le panier n'est jamais réaffiché en clair : « ••••••••AB3K ». */
    private static String maskedCode(String code) {
        return "••••••••" + code.substring(Math.max(0, code.length() - 4));
    }

}