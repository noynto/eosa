package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;

import me.noynto.eosa.shared.ImageId;

import java.util.ArrayList;
import java.util.List;

public record AddImagesToProduct(
        ProductProvider productProvider,
        ImageProvider imageProvider
) {

    public Product handle(Command command) throws ProductProvider.UnknownProduct {
        Product product = productProvider.read(command.productId());
        List<ImageId> imageIds = new ArrayList<>(product.getImageIds());
        for (Image image : command.images()) {
            Image uploaded = imageProvider.upload(image);
            imageIds.add(uploaded.getId());
        }
        product.setImageIds(imageIds);
        return productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            List<Image> images
    ) {
    }

}