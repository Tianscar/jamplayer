package com.tianscar.jamplayer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.Closeable;
import java.util.Objects;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

final class Utils {

    static final int DEFAULT_BUFFER_SIZE = 8192;
    static final AudioFormat DEFAULT_FORMAT = new AudioFormat(44100, 16, 2, true, false);
    static final DataLine.Info DEFAULT_INFO = new DataLine.Info(SourceDataLine.class, DEFAULT_FORMAT);

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

    public static AudioInputStream getSupportedAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        Objects.requireNonNull(sourceStream);
        AudioFormat sourceFormat = sourceStream.getFormat();

        int sampleSizeInBits = sourceFormat.getSampleSizeInBits();
        if (sampleSizeInBits == NOT_SPECIFIED) sampleSizeInBits = targetFormat.getSampleSizeInBits();
        int channels = sourceFormat.getChannels();
        if (channels == NOT_SPECIFIED) channels = targetFormat.getChannels();

        AudioFormat decodedFormat = new AudioFormat(
                sourceFormat.getSampleRate(),
                sampleSizeInBits,
                channels,
                true,
                sourceFormat.isBigEndian()
        );
        return AudioSystem.getAudioInputStream(targetFormat, AudioSystem.getAudioInputStream(decodedFormat, sourceStream));
    }

}
