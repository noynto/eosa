package me.noynto.eosa.application;

import me.noynto.eosa.application.DownloadVideoRange.Outcome;
import me.noynto.eosa.shared.VideoId;
import me.noynto.eosa.video.Video;
import me.noynto.eosa.video.VideoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadVideoRangeTest {

    // Plus d'une tranche, pour vérifier le découpage.
    static final int SIZE = DownloadVideoRange.SLICE_SIZE * 2 + 1000;

    final VideoId videoId = new VideoId("v1");
    final byte[] content = new byte[SIZE];

    @Mock VideoProvider videoProvider;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < SIZE; i++) {
            content[i] = (byte) (i % 251);
        }
        var video = new Video();
        video.setId(videoId);
        video.setFormat("video/mp4");
        video.setSize(SIZE);
        lenient().when(videoProvider.read(videoId)).thenReturn(Optional.of(video));
        lenient().when(videoProvider.readRange(any(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int offset = invocation.getArgument(1);
            int length = invocation.getArgument(2);
            return Arrays.copyOfRange(content, offset, Math.min(offset + length, SIZE));
        });
    }

    @Test
    void handle_returnsTheWholeVideoWithoutRange() {
        var result = handle(null);

        assertEquals(Outcome.FULL, result.outcome());
        assertEquals(0, result.start());
        assertEquals(SIZE - 1, result.end());
        assertEquals(SIZE, result.length());
        assertArrayEquals(content, read(result.content()));
    }

    @Test
    void handle_readsTheVideoInSlices() {
        var result = handle(null);

        int slices = 0;
        for (Iterator<byte[]> it = result.content(); it.hasNext(); it.next()) {
            slices++;
        }

        assertEquals(3, slices);
    }

    @Test
    void handle_returnsAClosedRange() {
        var result = handle("bytes=100-299");

        assertEquals(Outcome.PARTIAL, result.outcome());
        assertEquals(100, result.start());
        assertEquals(299, result.end());
        assertEquals(200, result.length());
        assertArrayEquals(Arrays.copyOfRange(content, 100, 300), read(result.content()));
    }

    @Test
    void handle_returnsAnOpenRangeUpToTheEnd() {
        var result = handle("bytes=" + (SIZE - 500) + "-");

        assertEquals(Outcome.PARTIAL, result.outcome());
        assertEquals(SIZE - 500, result.start());
        assertEquals(SIZE - 1, result.end());
        assertArrayEquals(Arrays.copyOfRange(content, SIZE - 500, SIZE), read(result.content()));
    }

    @Test
    void handle_returnsTheLastBytesOfASuffixRange() {
        var result = handle("bytes=-500");

        assertEquals(Outcome.PARTIAL, result.outcome());
        assertEquals(SIZE - 500, result.start());
        assertEquals(SIZE - 1, result.end());
    }

    @Test
    void handle_returnsTheWholeVideoOnASuffixLongerThanTheVideo() {
        var result = handle("bytes=-" + (SIZE + 10));

        assertEquals(Outcome.PARTIAL, result.outcome());
        assertEquals(0, result.start());
        assertEquals(SIZE - 1, result.end());
    }

    @Test
    void handle_cutsTheEndOfARangeBeyondTheVideo() {
        var result = handle("bytes=0-" + (SIZE * 2));

        assertEquals(Outcome.PARTIAL, result.outcome());
        assertEquals(SIZE - 1, result.end());
    }

    @Test
    void handle_crossesSlicesInARange() {
        int start = DownloadVideoRange.SLICE_SIZE - 10;
        int end = DownloadVideoRange.SLICE_SIZE + 10;

        var result = handle("bytes=" + start + "-" + end);

        assertArrayEquals(Arrays.copyOfRange(content, start, end + 1), read(result.content()));
    }

    @Test
    void handle_refusesARangeStartingAfterTheEnd() {
        var result = handle("bytes=" + SIZE + "-");

        assertEquals(Outcome.NOT_SATISFIABLE, result.outcome());
        assertNull(result.content());
        verify(videoProvider, never()).readRange(any(), anyInt(), anyInt());
    }

    @Test
    void handle_refusesARangeEndingBeforeItsStart() {
        assertEquals(Outcome.NOT_SATISFIABLE, handle("bytes=200-100").outcome());
    }

    @Test
    void handle_returnsTheWholeVideoOnARangeItCannotRead() {
        for (String range : new String[]{"bytes=-", "items=0-10", "bytes=0-10,20-30", "bytes=a-b", ""}) {
            assertEquals(Outcome.FULL, handle(range).outcome(), range);
        }
    }

    @Test
    void handle_returnsNothingToReadForAnEmptyVideo() {
        var video = new Video();
        video.setId(videoId);
        video.setSize(0);
        when(videoProvider.read(videoId)).thenReturn(Optional.of(video));

        var result = handle(null);

        assertEquals(Outcome.FULL, result.outcome());
        assertFalse(result.content().hasNext());
    }

    @Test
    void handle_throwsWhenTheVideoDoesNotExist() {
        var unknown = new VideoId("unknown");
        when(videoProvider.read(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> new DownloadVideoRange(videoProvider).handle(new DownloadVideoRange.Command(unknown, null)));
    }

    private DownloadVideoRange.Result handle(String range) {
        return new DownloadVideoRange(videoProvider).handle(new DownloadVideoRange.Command(videoId, range));
    }

    private static byte[] read(Iterator<byte[]> slices) {
        var out = new ByteArrayOutputStream();
        slices.forEachRemaining(out::writeBytes);
        return out.toByteArray();
    }

}
