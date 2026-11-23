package me.noynto.eosa.video;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Lit le codec de la piste vidéo d'un fichier MP4 sans le décoder : c'est le nom de la première « sample entry »
 * de la table {@code stsd} de la piste dont le gestionnaire est {@code vide} ({@code avc1} pour du H.264,
 * {@code hvc1} pour du H.265).
 * <p>
 * Le navigateur n'indique que le type du fichier (MP4), pas son contenu : un MP4 en H.265 est accepté par tous les
 * contrôles mais Firefox, entre autres, ne sait pas le lire sans décodeur matériel.
 */
public final class Mp4Codec {

    /** H.264 : le seul codec lu par tous les navigateurs (Firefox, Chrome, Safari et iOS). */
    public static final String H264 = "avc1";
    private static final String H264_VARIANT = "avc3";

    private Mp4Codec() {
    }

    /** Le nom (quatre lettres) du codec de la première piste vidéo, vide si le fichier n'en a pas ou n'est pas lisible. */
    public static Optional<String> videoCodec(byte[] mp4) {
        if (mp4 == null) {
            return Optional.empty();
        }
        Range moov = child(mp4, new Range(0, mp4.length), "moov");
        if (moov == null) {
            return Optional.empty();
        }
        int position = moov.start;
        while (position < moov.end) {
            Box box = boxAt(mp4, position, moov.end);
            if (box == null) {
                return Optional.empty();
            }
            if (box.type.equals("trak")) {
                Optional<String> codec = codecOfVideoTrack(mp4, box.payload);
                if (codec.isPresent()) {
                    return codec;
                }
            }
            position = box.payload.end;
        }
        return Optional.empty();
    }

    /** Vrai pour du H.264, quelle que soit la variante ({@code avc1} ou {@code avc3}). */
    public static boolean isH264(String codec) {
        return H264.equals(codec) || H264_VARIANT.equals(codec);
    }

    /** Le nom du codec tel qu'on le dit à l'administrateur. */
    public static String label(String codec) {
        return switch (codec) {
            case "hvc1", "hev1" -> "H.265 (HEVC)";
            case "av01" -> "AV1";
            case "vp09" -> "VP9";
            case "avc1", "avc3" -> "H.264";
            default -> codec;
        };
    }

    private static Optional<String> codecOfVideoTrack(byte[] mp4, Range trak) {
        Range mdia = child(mp4, trak, "mdia");
        if (mdia == null) {
            return Optional.empty();
        }
        Range hdlr = child(mp4, mdia, "hdlr");
        // Version et drapeaux (4 octets), pre_defined (4 octets), puis le type du gestionnaire.
        if (hdlr == null || hdlr.end - hdlr.start < 12 || !"vide".equals(text(mp4, hdlr.start + 8))) {
            return Optional.empty();
        }
        Range minf = child(mp4, mdia, "minf");
        Range stbl = minf == null ? null : child(mp4, minf, "stbl");
        Range stsd = stbl == null ? null : child(mp4, stbl, "stsd");
        // Version et drapeaux (4 octets), nombre d'entrées (4 octets), puis la première entrée : taille, type.
        if (stsd == null || stsd.end - stsd.start < 16) {
            return Optional.empty();
        }
        return Optional.of(text(mp4, stsd.start + 12));
    }

    /** La zone de contenu du premier enfant de ce type dans la zone donnée, ou {@code null}. */
    private static Range child(byte[] data, Range within, String type) {
        int position = within.start;
        while (position < within.end) {
            Box box = boxAt(data, position, within.end);
            if (box == null) {
                return null;
            }
            if (box.type.equals(type)) {
                return box.payload;
            }
            position = box.payload.end;
        }
        return null;
    }

    /** La boîte qui commence à cette position, ou {@code null} si elle est tronquée ou incohérente. */
    private static Box boxAt(byte[] data, int position, int limit) {
        if (limit - position < 8) {
            return null;
        }
        long size = unsignedInt(data, position);
        int header = 8;
        if (size == 1) {
            // Taille sur 64 bits après le type.
            if (limit - position < 16) {
                return null;
            }
            size = (unsignedInt(data, position + 8) << 32) | unsignedInt(data, position + 12);
            header = 16;
        } else if (size == 0) {
            // La boîte va jusqu'à la fin.
            size = limit - position;
        }
        if (size < header || position + size > limit) {
            return null;
        }
        return new Box(text(data, position + 4), new Range(position + header, (int) (position + size)));
    }

    private static long unsignedInt(byte[] data, int position) {
        return ((data[position] & 0xFFL) << 24) | ((data[position + 1] & 0xFFL) << 16)
                | ((data[position + 2] & 0xFFL) << 8) | (data[position + 3] & 0xFFL);
    }

    private static String text(byte[] data, int position) {
        return new String(data, position, 4, StandardCharsets.ISO_8859_1);
    }

    private record Range(int start, int end) {
    }

    private record Box(String type, Range payload) {
    }

}
