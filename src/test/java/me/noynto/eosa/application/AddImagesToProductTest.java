package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
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
class AddImagesToProductTest {

    @Mock ProductProvider productProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_uploadsEachImageAndUpdatesProduct() {
        var productId = new ProductId("prod1");
        var product = new Product();
        product.setId(productId);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var img1 = imageWith("img1");
        var img2 = imageWith("img2");
        when(imageProvider.upload(any()))
                .thenAnswer(inv -> uploadedWith("id-" + ((Image) inv.getArgument(0)).getName()));

        new AddImagesToProduct(productProvider, imageProvider).handle(
                new AddImagesToProduct.Command(productId, List.of(img1, img2))
        );

        verify(productProvider).write(argThat(p ->
                p.getImageIds().size() == 2 &&
                p.getImageIds().getFirst().value().equals("id-img1") &&
                p.getImageIds().get(1).value().equals("id-img2")
        ));
    }

    @Test
    void handle_preservesExistingImageIds() {
        var productId = new ProductId("prod1");
        var product = new Product();
        product.setId(productId);
        product.setImageIds(List.of(new ImageId("existing")));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageProvider.upload(any())).thenReturn(uploadedWith("new"));

        new AddImagesToProduct(productProvider, imageProvider).handle(
                new AddImagesToProduct.Command(productId, List.of(imageWith("new")))
        );

        verify(productProvider).write(argThat(p ->
                p.getImageIds().size() == 2 &&
                p.getImageIds().getFirst().value().equals("existing") &&
                p.getImageIds().get(1).value().equals("new")
        ));
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddImagesToProduct(productProvider, imageProvider).handle(
                        new AddImagesToProduct.Command(productId, List.of(imageWith("x")))
                )
        );
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