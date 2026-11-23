package me.noynto.eosa.video;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4CodecTest {

    @Test
    void videoCodec_readsH264() {
        assertEquals(Optional.of("avc1"), Mp4Codec.videoCodec(Mp4Samples.withVideoCodec("avc1")));
    }

    @Test
    void videoCodec_readsH265() {
        assertEquals(Optional.of("hvc1"), Mp4Codec.videoCodec(Mp4Samples.withVideoCodec("hvc1")));
    }

    @Test
    void videoCodec_skipsAnAudioTrackBeforeTheVideoTrack() {
        assertEquals(Optional.of("avc1"), Mp4Codec.videoCodec(Mp4Samples.withAudioThenVideo("avc1")));
    }

    @Test
    void videoCodec_isEmptyWithoutAVideoTrack() {
        assertEquals(Optional.empty(), Mp4Codec.videoCodec(Mp4Samples.withAudioOnly()));
    }

    @Test
    void videoCodec_isEmptyWithoutMoov() {
        assertEquals(Optional.empty(), Mp4Codec.videoCodec(Mp4Samples.withoutMoov()));
    }

    @Test
    void videoCodec_isEmptyForNullAndForBytesThatAreNotAnMp4() {
        assertEquals(Optional.empty(), Mp4Codec.videoCodec(null));
        assertEquals(Optional.empty(), Mp4Codec.videoCodec(new byte[100]));
        assertEquals(Optional.empty(), Mp4Codec.videoCodec(new byte[0]));
    }

    @Test
    void videoCodec_isEmptyForATruncatedFileAtEveryLength() {
        byte[] whole = Mp4Samples.withVideoCodec("avc1");
        for (int length = 0; length < whole.length; length++) {
            assertEquals(Optional.empty(), Mp4Codec.videoCodec(Arrays.copyOf(whole, length)), "longueur " + length);
        }
    }

    @Test
    void isH264_acceptsBothVariantsAndNothingElse() {
        assertTrue(Mp4Codec.isH264("avc1"));
        assertTrue(Mp4Codec.isH264("avc3"));
        assertFalse(Mp4Codec.isH264("hvc1"));
        assertFalse(Mp4Codec.isH264("av01"));
    }

    @Test
    void label_namesTheCommonCodecsAndFallsBackToTheFourLetters() {
        assertEquals("H.265 (HEVC)", Mp4Codec.label("hvc1"));
        assertEquals("H.265 (HEVC)", Mp4Codec.label("hev1"));
        assertEquals("AV1", Mp4Codec.label("av01"));
        assertEquals("xyz1", Mp4Codec.label("xyz1"));
    }

}
