package me.noynto.eosa.application;

import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddOptionToProductTest {

    @Mock ProductProvider productProvider;
    @Mock OptionProvider optionProvider;

    @Test
    void handle_appendsOptionAtEndByDefault() {
        var productId = new ProductId("abc");
        var product = new Product();
        product.setId(productId);
        product.setOptionIds(new ArrayList<>(List.of(new OptionId("type"))));
        var colorId = new OptionId("color");
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(optionProvider.read(colorId)).thenReturn(Optional.of(new Option()));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new AddOptionToProduct(productProvider, optionProvider).handle(
                new AddOptionToProduct.Command(productId, colorId)
        );

        assertEquals(List.of(new OptionId("type"), colorId), result.getOptionIds());
    }

    @Test
    void handle_insertsOptionAtGivenPosition() {
        var productId = new ProductId("abc");
        var product = new Product();
        product.setId(productId);
        product.setOptionIds(new ArrayList<>(List.of(new OptionId("type"), new OptionId("length"))));
        var colorId = new OptionId("color");
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(optionProvider.read(colorId)).thenReturn(Optional.of(new Option()));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new AddOptionToProduct(productProvider, optionProvider).handle(
                new AddOptionToProduct.Command(productId, colorId, 1)
        );

        assertEquals(List.of(new OptionId("type"), colorId, new OptionId("length")), result.getOptionIds());
    }

    @Test
    void handle_throwsWhenOptionAlreadyAssociated() {
        var productId = new ProductId("abc");
        var typeId = new OptionId("type");
        var product = new Product();
        product.setId(productId);
        product.setOptionIds(new ArrayList<>(List.of(typeId)));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(optionProvider.read(typeId)).thenReturn(Optional.of(new Option()));

        assertThrows(RuntimeException.class, () ->
                new AddOptionToProduct(productProvider, optionProvider).handle(
                        new AddOptionToProduct.Command(productId, typeId)
                )
        );
    }

    @Test
    void handle_throwsWhenOptionNotFound() {
        var productId = new ProductId("abc");
        var product = new Product();
        product.setId(productId);
        var unknownId = new OptionId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(optionProvider.read(unknownId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddOptionToProduct(productProvider, optionProvider).handle(
                        new AddOptionToProduct.Command(productId, unknownId)
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddOptionToProduct(productProvider, optionProvider).handle(
                        new AddOptionToProduct.Command(productId, new OptionId("color"))
                )
        );
    }

}
