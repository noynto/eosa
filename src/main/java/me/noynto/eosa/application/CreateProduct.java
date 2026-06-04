package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;

import java.math.BigDecimal;

public record CreateProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        Product product = new Product();
        product.setState(ProductState.DRAFTED);
        product.setName(command.name());
        return productProvider.write(product);
    }

    public record Command(
            String name
    ) {
    }

}