package me.noynto.eosa.video;

import me.noynto.eosa.shared.VideoId;

import java.util.Optional;

public interface VideoProvider {

    /** Écrit le contenu de la vidéo et lui donne son identifiant et sa taille. */
    Video write(Video video);

    /** Lit les métadonnées, sans le contenu. */
    Optional<Video> read(VideoId videoId);

    /** Lit une tranche du contenu : {@code length} octets à partir de {@code offset} (0 pour le premier octet), moins en fin de fichier. */
    byte[] readRange(VideoId videoId, int offset, int length);

    void delete(VideoId videoId);

}
