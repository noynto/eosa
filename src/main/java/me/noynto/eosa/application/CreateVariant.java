package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.product.VariantState;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

import java.util.Map;
import java.util.UUID;

public record CreateVariant(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.optionValues == null || command.optionValues.isEmpty()) {
            throw new RuntimeException("Au moins une valeur d'option est nécessaire pour créer un variant.");
        }

        Product product = this.productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        boolean alreadyExists = product.getVariants().stream()
                .anyMatch(v -> v.getOptionValues().equals(command.optionValues));
        if (alreadyExists) {
            throw new RuntimeException("Un variant avec cette combinaison de valeurs d'options existe déjà.");
        }

        Variant variant = new Variant();
        variant.setId(new VariantId(UUID.randomUUID().toString()));
        variant.setOptionValues(command.optionValues);
        variant.setState(VariantState.DRAFTED);

        product.getVariants().add(variant);

        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            Map<OptionId, OptionValueId> optionValues
    ) {
    }
}
