package com.tianscar.jamplayer;

import com.tianscar.javasound.sampled.AudioResourceLoader;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.tianscar.jamplayer.Utils.*;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

public class MusicPlayer {

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
        if (!isSourceAvailable()) throw new IllegalStateException("Please set data source first!");
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
        return isPrepared() ? microsecondsLength.get() : NOT_SPECIFIED;
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

    private final AtomicLong microsecondsLength = new AtomicLong(NOT_SPECIFIED);
    public void prepare() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        checkSourceAvailable();
        if (isPrepared()) return;
        synchronized (lock) {
            final AudioInputStream sourceStream;
            final AudioFileFormat sourceFileFormat;
            if (source instanceof File) {
                File file = (File) source;
                sourceStream = AudioSystem.getAudioInputStream(file);
                sourceFileFormat = AudioSystem.getAudioFileFormat(file);
            }
            else if (source instanceof String) {
                String name = (String) source;
                sourceStream = AudioResourceLoader.getAudioInputStream(resourceLoader, name);
                sourceFileFormat = AudioResourceLoader.getAudioFileFormat(resourceLoader, name);
            }
            else throw new IOException("Invalid source: " + source);
            Long duration = (Long) sourceFileFormat.properties().get("duration");
            if (duration == null) {
                if (sourceFileFormat.getFrameLength() != NOT_SPECIFIED &&
                        sourceFileFormat.getFormat().getFrameRate() != NOT_SPECIFIED) {
                    microsecondsLength.set((long) (((double) sourceFileFormat.getFrameLength() /
                            (double) sourceFileFormat.getFormat().getFrameRate()) * 1_000_000L));
                }
            }
            else microsecondsLength.set(duration);
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
