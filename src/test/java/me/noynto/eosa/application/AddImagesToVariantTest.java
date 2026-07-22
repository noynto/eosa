package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddImagesToVariantTest {

    @Mock ProductProvider productProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_uploadsEachImageAndUpdatesVariant() {
        var productId = new ProductId("abc");
        var variantId = new VariantId("v1");
        var product = productWithVariant(productId, variantId);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageProvider.upload(any()))
                .thenAnswer(inv -> uploadedWith("id-" + ((Image) inv.getArgument(0)).getName()));

        new AddImagesToVariant(productProvider, imageProvider).handle(
                new AddImagesToVariant.Command(productId, variantId, List.of(imageWith("img1"), imageWith("img2")))
        );

        verify(productProvider).write(argThat(p -> {
            var variant = p.getVariants().getFirst();
            return variant.getImageIds().size() == 2
                    && variant.getImageIds().getFirst().value().equals("id-img1")
                    && variant.getImageIds().get(1).value().equals("id-img2");
        }));
    }

    @Test
    void handle_throwsWhenVariantNotFound() {
        var productId = new ProductId("abc");
        var product = productWithVariant(productId, new VariantId("v1"));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new AddImagesToVariant(productProvider, imageProvider).handle(
                        new AddImagesToVariant.Command(productId, new VariantId("unknown"), List.of(imageWith("x")))
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddImagesToVariant(productProvider, imageProvider).handle(
                        new AddImagesToVariant.Command(productId, new VariantId("v1"), List.of(imageWith("x")))
                )
        );
    }

    private Product productWithVariant(ProductId productId, VariantId variantId) {
        var product = new Product();
        product.setId(productId);
        var variant = new Variant();
        variant.setId(variantId);
        product.setVariants(List.of(variant));
        return product;
    }

    private Image imageWith(String name) {
        var image = new Image();
        image.setName(name);
        return image;
    }

    private Image uploadedWith(String id) {
        var image = new Image();
        image.setId(new ImageId(id));
        return image;
    }

}
