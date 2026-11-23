package me.noynto.eosa.video;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Des MP4 minimaux (juste les boîtes que lit {@link Mp4Codec}) pour les tests. */
public final class Mp4Samples {

    private Mp4Samples() {
    }

    /** Un fichier à une piste vidéo dont la première entrée de {@code stsd} porte ce codec. */
    public static byte[] withVideoCodec(String codec) {
        return concat(box("ftyp", text("isom"), new byte[4]), moov(videoTrack(codec)));
    }

    /** Un fichier à une piste audio puis une piste vidéo. */
    public static byte[] withAudioThenVideo(String codec) {
        return concat(box("ftyp", text("isom"), new byte[4]), moov(track("soun", "mp4a"), videoTrack(codec)));
    }

    /** Un fichier sans piste vidéo. */
    public static byte[] withAudioOnly() {
        return concat(box("ftyp", text("isom"), new byte[4]), moov(track("soun", "mp4a")));
    }

    /** Un fichier sans la boîte {@code moov}. */
    public static byte[] withoutMoov() {
        return box("ftyp", text("isom"), new byte[4]);
    }

    private static byte[] videoTrack(String codec) {
        return track("vide", codec);
    }

    private static byte[] moov(byte[]... tracks) {
        return box("moov", concat(tracks));
    }

    private static byte[] track(String handler, String sampleEntry) {
        // hdlr : version et drapeaux, pre_defined, type du gestionnaire.
        byte[] hdlr = box("hdlr", new byte[8], text(handler), new byte[12]);
        // stsd : version et drapeaux, une entrée ; l'entrée : sa taille, son type, puis un contenu quelconque.
        byte[] entry = box(sampleEntry, new byte[8]);
        byte[] stsd = box("stsd", new byte[4], new byte[]{0, 0, 0, 1}, entry);
        byte[] stbl = box("stbl", stsd);
        byte[] minf = box("minf", stbl);
        byte[] mdia = box("mdia", hdlr, minf);
        return box("trak", mdia);
    }

    private static byte[] box(String type, byte[]... payload) {
        byte[] content = concat(payload);
        var out = new ByteArrayOutputStream();
        int size = 8 + content.length;
        out.write(size >>> 24);
        out.write(size >>> 16);
        out.write(size >>> 8);
        out.write(size);
        out.writeBytes(text(type));
        out.writeBytes(content);
        return out.toByteArray();
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

}
