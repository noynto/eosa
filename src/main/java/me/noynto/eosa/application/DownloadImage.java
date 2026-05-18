package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.ImageId;

import java.util.Optional;

public record DownloadImage(
        ImageProvider imageProvider
) {

    public Optional<Image> handle(Command command) {
        return imageProvider.download(command.imageId());
    }

    public record Command(
            ImageId imageId
    ) {
    }

}