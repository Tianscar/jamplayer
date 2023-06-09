package com.tianscar.jamplayer.test;

import com.tianscar.jamplayer.MusicEvent;
import com.tianscar.jamplayer.MusicListener;
import com.tianscar.jamplayer.MusicPlayer;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.MixerProvider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

public class MixerExample {

    private static final AtomicInteger playingLeft = new AtomicInteger(2);

    public static void main(String[] args) {
        try {
            MixerProvider softMixerSpi =
                    (MixerProvider) Class.forName("com.sun.media.sound.SoftMixingMixerProvider").getConstructor().newInstance();
            Mixer mixer = softMixerSpi.getMixer(softMixerSpi.getMixerInfo()[0]);
            MusicPlayer player1 = new MusicPlayer(mixer);
            MusicPlayer player2 = new MusicPlayer(mixer);
            MusicListener listener = new MusicListener() {
                @Override
                public void update(MusicEvent event) {
                    if (event.getType() == MusicEvent.Type.STOP) {
                        playingLeft.set(playingLeft.get() - 1);
                    }
                }
            };
            player1.addMusicListener(listener);
            player2.addMusicListener(listener);
            player1.setVolume(1, 0);
            player2.setVolume(0, 1);
            player1.setDataSource(Thread.currentThread().getContextClassLoader(), "fbodemo1.wav");
            player2.setDataSource(Thread.currentThread().getContextClassLoader(), "fbodemo1.aiff");
            player1.prepare();
            player2.prepare();
            player1.start();
            Thread.sleep(2000);
            player2.start();
            while (playingLeft.get() > 0) {
                Thread.sleep(1000);
            }
        }
        catch (UnsupportedAudioFileException | LineUnavailableException | IOException | InterruptedException |
               ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("LoopTest failed: ", e);
        }
    }

}
