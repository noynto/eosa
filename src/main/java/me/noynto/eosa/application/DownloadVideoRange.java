package me.noynto.eosa.application;

import me.noynto.eosa.shared.VideoId;
import me.noynto.eosa.video.Video;
import me.noynto.eosa.video.VideoProvider;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit une vidéo, en entier ou selon l'en-tête {@code Range}. Le contenu est rendu par tranches, chacune lue par une requête
 * à part : la connexion à la base n'est jamais gardée pendant l'envoi à un client lent.
 */
public record DownloadVideoRange(
        VideoProvider videoProvider
) {

    public static final int SLICE_SIZE = 256 * 1024;

    // Une seule plage : « bytes=a-b », « bytes=a- » (jusqu'à la fin) ou « bytes=-n » (les n derniers octets).
    private static final Pattern RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");

    public Result handle(Command command) {
        Video video = videoProvider.read(command.videoId())
                .orElseThrow(() -> new RuntimeException("La vidéo " + command.videoId().value() + " n'existe pas."));
        int size = video.getSize();

        Matcher matcher = command.range() == null ? null : RANGE.matcher(command.range().trim());
        if (matcher == null || !matcher.matches() || (matcher.group(1).isEmpty() && matcher.group(2).isEmpty())) {
            // Pas de plage, ou une plage que l'on ne sait pas lire : la vidéo entière, ce que la norme permet.
            return new Result(Outcome.FULL, video, 0, size - 1, slices(video, 0, size - 1));
        }

        long start;
        long end;
        try {
            if (matcher.group(1).isEmpty()) {
                long suffix = Long.parseLong(matcher.group(2));
                start = Math.max(0, size - suffix);
                end = size - 1;
            } else {
                start = Long.parseLong(matcher.group(1));
                end = matcher.group(2).isEmpty() ? size - 1 : Math.min(Long.parseLong(matcher.group(2)), size - 1);
            }
        } catch (NumberFormatException e) {
            return new Result(Outcome.NOT_SATISFIABLE, video, 0, size - 1, null);
        }
        if (size == 0 || start >= size || start > end) {
            return new Result(Outcome.NOT_SATISFIABLE, video, 0, size - 1, null);
        }
        return new Result(Outcome.PARTIAL, video, (int) start, (int) end, slices(video, (int) start, (int) end));
    }

    private Iterator<byte[]> slices(Video video, int start, int end) {
        return new Iterator<>() {
            private int next = start;

            @Override
            public boolean hasNext() {
                return next <= end;
            }

            @Override
            public byte[] next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                int length = Math.min(SLICE_SIZE, end - next + 1);
                byte[] slice = videoProvider.readRange(video.getId(), next, length);
                next += length;
                return slice;
            }
        };
    }

    public enum Outcome {
        FULL,
        PARTIAL,
        NOT_SATISFIABLE
    }

    /** {@code start} et {@code end} sont inclus, comme dans {@code Content-Range} ; {@code content} est nul si la plage est insatisfaisable. */
    public record Result(
            Outcome outcome,
            Video video,
            int start,
            int end,
            Iterator<byte[]> content
    ) {

        public int length() {
            return end - start + 1;
        }

    }

    public record Command(
            VideoId videoId,
            String range
    ) {
    }

}
