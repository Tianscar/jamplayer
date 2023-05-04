package com.tianscar.jamplayer;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.ogg.audio.OggAudioStatistics;
import org.gagravarr.vorbis.VorbisFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
        }
    }

    public static float clamp(final float value, final float min, final float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    public static long clamp(final long value, final long min, final long max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    public static byte[] adjustVolume(byte[] samples, int offset, int length, float leftVolume, float rightVolume) {
        short[] buf = new short[4];
        short left, right;
        for (int i = offset; i < offset + length; i += 4) {

            buf[0] = (short) ((samples[i + 1] & 0xFF) << 8);
            buf[1] = (short) (samples[i] & 0xFF);
            buf[2] = (short) ((samples[i + 3] & 0xFF) << 8);
            buf[3] = (short) (samples[i + 2] & 0xFF);

            left  = (short) ((buf[0] | buf[1]) * leftVolume);
            right = (short) ((buf[2] | buf[3]) * rightVolume);

            samples[i]     = (byte) left;
            samples[i + 1] = (byte) (left >> 8);
            samples[i + 2] = (byte) right;
            samples[i + 3] = (byte) (right >> 8);
        }
        return samples;
    }

    public static AudioInputStream getSupportedAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        Objects.requireNonNull(audioInputStream);
        AudioFormat baseFormat = audioInputStream.getFormat();
        return AudioSystem.getAudioInputStream(targetFormat,
                AudioSystem.getAudioInputStream(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(), false), audioInputStream));
    }

    public static long getMP3MicrosecondLength(InputStream in, int tn) {
        Bitstream bitstream = new Bitstream(in);
        Header header;
        try {
            header = bitstream.readFrame();
        } catch (BitstreamException e) {
            return -1;
        }
        return (long) (header.total_ms(tn) * 1000L);
    }

    public static long getOGGMicrosecondLength(InputStream in) throws IOException {
        VorbisFile vorbisFile = new VorbisFile(new OggFile(in));
        OggAudioStatistics statistics = new OggAudioStatistics(vorbisFile, vorbisFile);
        statistics.calculate();
        vorbisFile.close();
        return (long) (statistics.getDurationSeconds() * 1_000_000L);
    }

}
