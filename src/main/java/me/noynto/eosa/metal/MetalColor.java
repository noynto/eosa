package me.noynto.eosa.metal;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.MetalColorId;

public class MetalColor {
    private MetalColorId id;
    private String name;
    private ImageId imageId;

    public MetalColor() {
    }

    public MetalColorId getId() {
        return id;
    }

    public void setId(MetalColorId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImageId getImageId() {
        return imageId;
    }

    public void setImageId(ImageId imageId) {
        this.imageId = imageId;
    }
}
