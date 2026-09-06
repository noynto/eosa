package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.cart.SelectedCharm;
import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.MetalColorId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record AddJewelToCart(
        CartProvider cartProvider,
        JewelProvider jewelProvider,
        MetalColorProvider metalColorProvider,
        CharmProvider charmProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Cart handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        var jewel = jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " n'existe pas."));

        MetalColor metalColor = command.metalColorId != null && command.metalColorId.value() != null
                ? metalColorProvider.read(command.metalColorId).orElse(null)
                : null;

        List<CharmId> requestedCharmIds = command.charmIds() != null ? command.charmIds() : List.of();
        List<SelectedCharm> selectedCharms = requestedCharmIds.stream()
                .map(charmProvider::read)
                .flatMap(Optional::stream)
                .map(charm -> new SelectedCharm(charm.getId(), charm.getName(), charm.getPrice(), charm.getImageId()))
                .toList();
        Set<String> requestedCharmIdValues = selectedCharms.stream().map(c -> c.charmId().value()).collect(Collectors.toSet());

        List<CartItem> items = new ArrayList<>(cart.getItems());

        var existing = items.stream()
                .filter(i -> i.jewelId().equals(command.jewelId))
                .filter(i -> Objects.equals(i.metalColorId(), command.metalColorId))
                .filter(i -> i.charms().stream().map(c -> c.charmId().value()).collect(Collectors.toSet()).equals(requestedCharmIdValues))
                .findFirst();

        if (existing.isPresent()) {
            items.remove(existing.get());
            items.add(new CartItem(
                    existing.get().id(),
                    existing.get().jewelId(),
                    existing.get().name(),
                    existing.get().price(),
                    existing.get().imageId(),
                    existing.get().quantity() + 1,
                    existing.get().metalColorId(),
                    existing.get().metalColorName(),
                    existing.get().metalColorImageId(),
                    existing.get().charms()
            ));
        } else {
            var imageId = jewel.getImageIds().isEmpty() ? null : jewel.getImageIds().getFirst();
            items.add(new CartItem(
                    new CartItemId(UUID.randomUUID().toString()),
                    command.jewelId,
                    jewel.getName(),
                    jewel.getPrice(),
                    imageId,
                    1,
                    metalColor != null ? metalColor.getId() : null,
                    metalColor != null ? metalColor.getName() : null,
                    metalColor != null ? metalColor.getImageId() : null,
                    selectedCharms
            ));
        }

        cart.setItems(items);
        cart.applyShippingRule(shippingRuleProvider.get());
        return cartProvider.write(cart);
    }

    public record Command(
            CartId cartId,
            JewelId jewelId,
            MetalColorId metalColorId,
            List<CharmId> charmIds
    ) {
    }

}
