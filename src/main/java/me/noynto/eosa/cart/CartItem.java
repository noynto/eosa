package me.noynto.eosa.cart;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;

public record CartItem(
        ProductId productId,
        String name,
        BigDecimal price,
        ImageId imageId,
        int quantity
) {
}