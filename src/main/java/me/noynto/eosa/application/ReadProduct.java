package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;

public record ReadProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) throws ProductProvider.UnknownProduct {
        return productProvider.read(command.productId());
    }

    public record Command(
            ProductId productId
    ) {
    }

}
