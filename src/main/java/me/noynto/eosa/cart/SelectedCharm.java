package me.noynto.eosa.cart;

import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;

import java.math.BigDecimal;

public record SelectedCharm(
        CharmId charmId,
        String name,
        BigDecimal price,
        ImageId imageId
) {
}
