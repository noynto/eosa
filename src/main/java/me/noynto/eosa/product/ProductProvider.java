package me.noynto.eosa.product;

import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface ProductProvider {

    Stream<ProductId> readIds(Search search);

    Optional<Product> read(ProductId productId);

    Product write(Product product);

    record Search(
            Set<ProductState> states,
            OptionId optionId,
            OptionValueId optionValueId
    ) {
        public Search(Set<ProductState> states) {
            this(states, null, null);
        }
    }

}
