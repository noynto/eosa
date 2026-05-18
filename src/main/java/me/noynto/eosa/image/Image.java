package me.noynto.eosa.image;

import me.noynto.eosa.shared.ImageId;

import java.io.InputStream;

public class Image {
    private ImageId id;
    private String name;
    private String format;
    private InputStream content;

    public Image() {
    }

    public ImageId getId() {
        return id;
    }

    public void setId(ImageId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }
}
