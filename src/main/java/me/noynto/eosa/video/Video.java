package me.noynto.eosa.video;

import me.noynto.eosa.shared.VideoId;

import java.util.Set;

/** Les métadonnées d'une vidéo ; le contenu ne se lit que par plages (voir {@link VideoProvider#readRange}). */
public class Video {

    // Seul format accepté : lu partout (Safari et iOS compris), sans piste audio dans la boutique.
    public static final Set<String> ACCEPTED_FORMATS = Set.of("video/mp4");

    public static final int MAX_SIZE = 8 * 1024 * 1024;

    private VideoId id;
    private String name;
    private String format;
    private int size;
    private byte[] content;

    public Video() {
    }

    public VideoId getId() {
        return id;
    }

    public void setId(VideoId id) {
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /** Renseigné seulement pour l'écriture ; la lecture ne le remplit jamais. */
    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
