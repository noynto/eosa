package me.noynto.eosa.product;

import me.noynto.eosa.shared.ProductId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface ProductProvider {

    Stream<ProductId> readIds(Set<ProductState> states);

    Optional<Product> read(ProductId productId);

    Product write(Product product);

}
