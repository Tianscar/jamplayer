package com.tianscar.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tianscar.audio.Utils.*;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

public class MusicPlayer {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final AudioFormat DEFAULT_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
            44100, 16, 2, 4, 44100, false);
    private static final DataLine.Info DEFAULT_INFO = new DataLine.Info(SourceDataLine.class, DEFAULT_FORMAT);

    private final Mixer mixer;
    private final AudioFormat playbackFormat;
    private final DataLine.Info playbackInfo;
    private final int streamBufferSize;

    private volatile SourceDataLine sourceDataLine = null;
    private volatile AudioInputStream audioInputStream = null;

    private final List<MusicListener> listeners = Collections.synchronizedList(new LinkedList<>());

    private final LineListener broadcastEvent = new LineListener() {
        @Override
        public void update(LineEvent event) {
            if (event.getType().equals(LineEvent.Type.START)) broadcastEvent(new MusicEvent(
                    MusicPlayer.this, MusicEvent.Type.START, event.getFramePosition()));
            else if (event.getType().equals(LineEvent.Type.STOP)) broadcastEvent(new MusicEvent(
                    MusicPlayer.this, MusicEvent.Type.STOP, event.getFramePosition()));
        }
    };
    private void broadcastEvent(MusicEvent event) {
        synchronized (listeners) {
            for (MusicListener listener : listeners) {
                listener.update(event);
            }
        }
    }

    private volatile boolean paused = false;

    private volatile float leftVolume = 1.0f;
    private volatile float rightVolume = 1.0f;

    private final Object lock = new byte[0];
    private volatile Object source = null;
    private volatile ClassLoader resourceLoader = null;

    public float getLeftVolume() {
        return leftVolume;
    }

    public float getRightVolume() {
        return rightVolume;
    }

    public void setLeftVolume(float leftVolume) {
        this.leftVolume = clamp(leftVolume, 0, 1);
    }

    public void setRightVolume(float rightVolume) {
        this.rightVolume = clamp(rightVolume, 0, 1);
    }

    public void setVolume(float leftVolume, float rightVolume) {
        setLeftVolume(leftVolume);
        setRightVolume(rightVolume);
    }

    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    public MusicPlayer(Mixer mixer, AudioFormat playbackFormat, int streamBufferSize) {
        if (streamBufferSize < 0) throw new ArrayIndexOutOfBoundsException(streamBufferSize);
        this.mixer = mixer;
        if (playbackFormat == null) {
            this.playbackFormat = DEFAULT_FORMAT;
            this.playbackInfo = DEFAULT_INFO;
        }
        else {
            this.playbackFormat = playbackFormat;
            this.playbackInfo = new DataLine.Info(SourceDataLine.class, playbackFormat);
        }
        this.streamBufferSize = streamBufferSize;
    }

    public MusicPlayer(Mixer mixer, AudioFormat playbackFormat) {
        this(mixer, playbackFormat, DEFAULT_BUFFER_SIZE);
    }

    public MusicPlayer(AudioFormat playbackFormat) {
        this(null, playbackFormat);
    }

    public MusicPlayer(Mixer mixer) {
        this(mixer, null);
    }

    public MusicPlayer() {
        this(null, null);
    }

    public MusicPlayer(int streamBufferSize) {
        this(null, null, streamBufferSize);
    }

    private boolean isSourceAvailable() {
        return source != null;
    }

    private void checkSourceAvailable() {
        if (!isSourceAvailable()) throw new IllegalStateException("Please set audio source first!");
    }

    public void setDataSource(ClassLoader classLoader, String resource) {
        if (isPrepared()) throw new IllegalStateException("You need to call the function before prepared");
        synchronized (lock) {
            this.resourceLoader = classLoader;
            this.source = resource;
        }
    }

    public void setDataSource(File file) {
        if (isPrepared()) throw new IllegalStateException("You need to call the function before prepared");
        synchronized (lock) {
            this.resourceLoader = null;
            this.source = file;
        }
    }

    public Mixer getMixer() {
        return mixer;
    }

    private static final AtomicInteger nextSerialNumber = new AtomicInteger();
    private static int serialNumber() {
        return nextSerialNumber.getAndIncrement();
    }

    public void seekToFrames(long framePosition) throws IOException {
        checkPrepared();
        if (isPlaying()) throw new IllegalStateException("You need to call the function before playing");
        audioInputStream.skip(framePosition * playbackFormat.getFrameSize());
    }

    public void seekToMicroseconds(long microsecondPosition) throws IOException {
        seekToFrames((long) (microsecondPosition / 1000_000L * playbackFormat.getFrameRate()));
    }

    public boolean isPlaying() {
        return isPrepared() && sourceDataLine.isOpen();
    }

    public void start() throws LineUnavailableException {
        checkPrepared();
        paused = false;
        if (sourceDataLine.isOpen()) return;
        sourceDataLine.open();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                sourceDataLine.start();
                byte[] buffer = new byte[streamBufferSize];
                int read = 0;
                while (sourceDataLine.isOpen() && read != -1) {
                    if (!paused) {
                        try {
                            read = audioInputStream.read(buffer, 0, buffer.length);
                        } catch (IOException e) {
                            stop();
                            return;
                        }
                        adjustVolume(buffer, 0, read, leftVolume, rightVolume);
                        if (read != -1) sourceDataLine.write(buffer, 0, read);
                    }
                }
                stop();
            }
        }, "MusicPlayback-" + serialNumber());
        t.setDaemon(true);
        t.start();
    }

    public void pause() {
        checkPrepared();
        paused = true;
    }

    public void stop() {
        checkPrepared();
        if (!isPrepared()) return;
        synchronized (lock) {
            if (!isPlaying()) return;
            if (audioInputStream != null) {
                try {
                    audioInputStream.close();
                    audioInputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            SourceDataLine tmp = sourceDataLine;
            sourceDataLine = null;
            tmp.drain();
            tmp.stop();
            tmp.removeLineListener(broadcastEvent);
            tmp.close();
        }
    }

    public SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }

    public AudioFormat getPlaybackFormat() {
        return playbackFormat;
    }

    public int getStreamBufferSize() {
        return streamBufferSize;
    }

    public int getDataLineBufferSize() {
        return isPrepared() ? sourceDataLine.getBufferSize() : 0;
    }

    public long getMicrosecondLength() {
        return isPrepared() ? microsecondsLength : NOT_SPECIFIED;
    }

    public int getFramePosition() {
        return isPrepared() ? sourceDataLine.getFramePosition() : 0;
    }

    public long getLongFramePosition() {
        return isPrepared() ? sourceDataLine.getLongFramePosition() : 0;
    }

    public long getMicrosecondPosition() {
        return isPrepared() ? sourceDataLine.getMicrosecondPosition() : 0;
    }

    public DataLine.Info getPlaybackLineInfo() {
        return playbackInfo;
    }

    private volatile long microsecondsLength = NOT_SPECIFIED;
    public void prepare() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        checkSourceAvailable();
        if (isPrepared()) return;
        synchronized (lock) {
            final AudioInputStream sourceStream;
            final String encoding;
            final long audioLength;
            final InputStream checkStream;
            final AudioFileFormat checkFormat;
            if (source instanceof File) {
                File file = (File) source;
                sourceStream = AudioSystem.getAudioInputStream(file);
                encoding = sourceStream.getFormat().getEncoding().toString();
                if (encoding.startsWith("MPEG")) {
                    audioLength = file.length();
                    checkStream = Files.newInputStream(file.toPath());
                    checkFormat = null;
                }
                else if (encoding.equals("FLAC") || encoding.equals("VORBISENC")) {
                    audioLength = 0;
                    checkStream = null;
                    checkFormat = AudioSystem.getAudioFileFormat(file);
                }
                else {
                    audioLength = 0;
                    checkStream = null;
                    checkFormat = null;
                }
            }
            else if (source instanceof String) {
                String resourceName = (String) source;
                InputStream resourceStream = resourceLoader.getResourceAsStream(resourceName);
                if (resourceStream == null) throw new IOException("Unable to load resource '" + source + "' with ClassLoader " + resourceLoader);
                sourceStream = AudioSystem.getAudioInputStream(resourceStream);
                encoding = sourceStream.getFormat().getEncoding().toString();
                URL resourceURL = resourceLoader.getResource(resourceName);
                if (resourceURL == null) throw new IOException("Unable to load resource '" + source + "' with ClassLoader " + resourceLoader);
                if (encoding.startsWith("MPEG")) {
                    URLConnection connection = resourceURL.openConnection();
                    audioLength = connection.getContentLengthLong();
                    checkStream = connection.getInputStream();
                    checkFormat = null;
                }
                else if (encoding.startsWith("VORBISENC")) {
                    audioLength = 0;
                    checkStream = resourceURL.openStream();
                    checkFormat = null;
                }
                else if (encoding.equals("FLAC")) {
                    audioLength = 0;
                    checkStream = null;
                    checkFormat = AudioSystem.getAudioFileFormat(resourceURL);
                }
                else {
                    audioLength = 0;
                    checkStream = null;
                    checkFormat = null;
                }
            }
            else throw new IOException("Invalid source: " + source);
            if (encoding.startsWith("MPEG")) {
                microsecondsLength = getMP3MicrosecondLength(checkStream, (int) clamp(audioLength, 0, Integer.MAX_VALUE));
                closeQuietly(checkStream);
            }
            else if ((encoding.equals("VORBISENC") && checkFormat != null) || encoding.equals("FLAC")) {
                Long duration = (Long) Objects.requireNonNull(checkFormat).properties().get("duration");
                microsecondsLength = duration == null ? -1 : duration;
            }
            else if (encoding.equals("VORBISENC")) {
                microsecondsLength = getOggMicrosecondLength(checkStream); // Already closed
            }
            long frameLength = sourceStream.getFrameLength();
            if (sourceStream.getFrameLength() != NOT_SPECIFIED && microsecondsLength == NOT_SPECIFIED)
                microsecondsLength = (long) (frameLength / sourceStream.getFormat().getFrameRate() * 1_000_000L);

            if (audioInputStream != null) audioInputStream.close();
            audioInputStream = getSupportedAudioInputStream(playbackFormat, sourceStream);
            SourceDataLine tmp = (SourceDataLine) (mixer == null ? AudioSystem.getLine(playbackInfo) : mixer.getLine(playbackInfo));
            tmp.addLineListener(broadcastEvent);
            sourceDataLine = tmp;
            broadcastEvent(new MusicEvent(this, MusicEvent.Type.PREPARE, NOT_SPECIFIED));
        }
    }

    public boolean isPrepared() {
        return sourceDataLine != null;
    }

    private void checkPrepared() {
        if (!isPrepared()) throw new IllegalStateException("Please prepare first!");
    }

    public void addMusicListener(MusicListener listener) {
        listeners.add(listener);
    }

    public void removeMusicListener(MusicListener listener) {
        listeners.remove(listener);
    }

}
