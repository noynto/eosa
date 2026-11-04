package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.ImageId;

import java.util.Optional;

public record DownloadImage(
        ImageProvider imageProvider
) {

    public Result handle(Command command) {
        return new Result(imageProvider.download(command.imageId())
                .orElseThrow(() -> new RuntimeException("L'image " + command.imageId().value() + " n'existe pas.")));
    }

    public record Result(
            Image image
    ) {
    }

    public record Command(
            ImageId imageId
    ) {
    }

}