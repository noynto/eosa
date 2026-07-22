package me.noynto.eosa.application;

import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;

import java.util.List;
import java.util.Set;

public record ReadProductIds(
        ProductProvider productProvider
) {

    public List<ProductId> handle(Query query) {
        var stream = productProvider.readIds(new ProductProvider.Search(query.states, query.optionId, query.optionValueId));
        if (query.limit != null) {
            stream = stream.limit(query.limit);
        }
        return stream.toList();
    }

    public record Query(
            Set<ProductState> states,
            OptionId optionId,
            OptionValueId optionValueId,
            Integer limit
    ) {
        public Query(Set<ProductState> states) {
            this(states, null, null, null);
        }

        public Query(Set<ProductState> states, OptionId optionId, OptionValueId optionValueId) {
            this(states, optionId, optionValueId, null);
        }
    }

}
