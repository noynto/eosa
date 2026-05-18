package me.noynto.eosa.product;

import me.noynto.eosa.shared.ProductId;

import java.util.stream.Stream;

public interface ProductProvider {

    Stream<ProductId> readIds();

    Product read(ProductId productId) throws UnknownProduct;

    Product write(Product product);

    class UnknownProduct extends Exception {
        private final ProductId productId;

        public UnknownProduct(ProductId productId) {
            this.productId = productId;
        }

        public ProductId getProductId() {
            return productId;
        }
    }
}
