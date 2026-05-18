package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;

import java.math.BigDecimal;

public record CreateProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        Product product = new Product();
        product.setName(command.name());
        product.setDescription(command.description());
        product.setPrice(command.price());
        return productProvider.write(product);
    }

    public record Command(
            String name,
            String description,
            BigDecimal price
    ) {
    }

}