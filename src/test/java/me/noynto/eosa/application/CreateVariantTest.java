package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.product.VariantState;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateVariantTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_appendsVariantToProduct() {
        var productId = new ProductId("abc");
        var product = new Product();
        product.setId(productId);
        product.setVariants(new ArrayList<>());
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var optionValues = Map.of(new OptionId("color"), new OptionValueId("gold"));
        var result = new CreateVariant(productProvider).handle(
                new CreateVariant.Command(productId, optionValues)
        );

        assertEquals(1, result.getVariants().size());
        assertEquals(optionValues, result.getVariants().getFirst().getOptionValues());
        assertEquals(VariantState.DRAFTED, result.getVariants().getFirst().getState());
    }

    @Test
    void handle_throwsWhenCombinationAlreadyExists() {
        var productId = new ProductId("abc");
        var optionValues = Map.of(new OptionId("color"), new OptionValueId("gold"));
        var existing = new Variant();
        existing.setOptionValues(optionValues);
        var product = new Product();
        product.setId(productId);
        product.setVariants(new ArrayList<>(java.util.List.of(existing)));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new CreateVariant(productProvider).handle(
                        new CreateVariant.Command(productId, optionValues)
                )
        );
    }

    @Test
    void handle_throwsWhenOptionValuesEmpty() {
        assertThrows(RuntimeException.class, () ->
                new CreateVariant(productProvider).handle(
                        new CreateVariant.Command(new ProductId("abc"), Map.of())
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new CreateVariant(productProvider).handle(
                        new CreateVariant.Command(productId, Map.of(new OptionId("color"), new OptionValueId("gold")))
                )
        );
    }

}
