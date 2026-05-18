package me.noynto.eosa.image;

import me.noynto.eosa.shared.ImageId;

import java.util.Optional;

public interface ImageProvider {

    Image upload(Image image);

    Optional<Image> download(ImageId imageId);

}