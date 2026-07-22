package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AddVariantToCart(
        CartProvider cartProvider,
        ProductProvider productProvider,
        OptionProvider optionProvider,
        CharmProvider charmProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Cart handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.variantId == null || command.variantId.value() == null) {
            throw new RuntimeException("L'identifiant du variant est nécessaire.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        Product product = productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        Variant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(command.variantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Le variant " + command.variantId.value() + " n'existe pas sur ce produit."));

        Charm charm = null;
        if (command.charmId != null) {
            charm = charmProvider.read(command.charmId)
                    .orElseThrow(() -> new RuntimeException("La breloque " + command.charmId.value() + " n'existe pas."));
        }

        List<CartItem> items = new ArrayList<>(cart.getItems());

        var existing = items.stream()
                .filter(i -> i.variantId().equals(command.variantId) && Objects.equals(i.charmId(), command.charmId))
                .findFirst();

        if (existing.isPresent()) {
            items.remove(existing.get());
            items.add(new CartItem(
                    existing.get().variantId(),
                    existing.get().charmId(),
                    existing.get().name(),
                    existing.get().price(),
                    existing.get().charmAdditionalPrice(),
                    existing.get().imageId(),
                    existing.get().quantity() + 1
            ));
        } else {
            String name = composeName(product, variant);
            ImageId imageId = resolveImageId(product, variant);
            items.add(new CartItem(
                    command.variantId,
                    command.charmId,
                    name,
                    variant.getPrice(),
                    charm == null ? null : charm.getAdditionalPrice(),
                    imageId,
                    1
            ));
        }

        cart.setItems(items);
        cart.applyShippingRule(shippingRuleProvider.get());
        return cartProvider.write(cart);
    }

    private String composeName(Product product, Variant variant) {
        List<String> valueLabels = new ArrayList<>();
        for (OptionId optionId : product.getOptionIds()) {
            var optionValueId = variant.getOptionValues().get(optionId);
            if (optionValueId == null) {
                continue;
            }
            Optional<Option> option = optionProvider.read(optionId);
            option.flatMap(o -> o.getValues().stream()
                            .filter(v -> v.getId().equals(optionValueId))
                            .findFirst())
                    .ifPresent(v -> valueLabels.add(v.getLabel()));
        }
        if (valueLabels.isEmpty()) {
            return product.getName();
        }
        return product.getName() + " — " + String.join(", ", valueLabels);
    }

    private ImageId resolveImageId(Product product, Variant variant) {
        if (!variant.getImageIds().isEmpty()) {
            return variant.getImageIds().getFirst();
        }
        if (product.getDefaultVariantId() != null) {
            return product.getVariants().stream()
                    .filter(v -> v.getId().equals(product.getDefaultVariantId()))
                    .findFirst()
                    .filter(v -> !v.getImageIds().isEmpty())
                    .map(v -> v.getImageIds().getFirst())
                    .orElse(null);
        }
        return null;
    }

    public record Command(
            CartId cartId,
            ProductId productId,
            VariantId variantId,
            CharmId charmId
    ) {
        public Command(CartId cartId, ProductId productId, VariantId variantId) {
            this(cartId, productId, variantId, null);
        }
    }

}
