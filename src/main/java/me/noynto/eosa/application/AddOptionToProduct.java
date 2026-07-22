package me.noynto.eosa.application;

import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.ProductId;

import java.util.ArrayList;
import java.util.List;

public record AddOptionToProduct(
        ProductProvider productProvider,
        OptionProvider optionProvider
) {

    public Product handle(Command command) {
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.optionId == null || command.optionId.value() == null) {
            throw new RuntimeException("L'identifiant de l'option est nécessaire.");
        }

        Product product = this.productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        this.optionProvider.read(command.optionId)
                .orElseThrow(() -> new RuntimeException("L'option " + command.optionId.value() + " n'existe pas."));

        if (product.getOptionIds().contains(command.optionId)) {
            throw new RuntimeException("L'option " + command.optionId.value() + " est déjà associée à ce produit.");
        }

        List<OptionId> optionIds = new ArrayList<>(product.getOptionIds());
        if (command.position != null && command.position >= 0 && command.position <= optionIds.size()) {
            optionIds.add(command.position, command.optionId);
        } else {
            optionIds.add(command.optionId);
        }
        product.setOptionIds(optionIds);

        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            OptionId optionId,
            Integer position
    ) {
        public Command(ProductId productId, OptionId optionId) {
            this(productId, optionId, null);
        }
    }
}
