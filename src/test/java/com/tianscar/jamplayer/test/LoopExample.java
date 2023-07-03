package com.tianscar.jamplayer.test;

import com.tianscar.jamplayer.MusicEvent;
import com.tianscar.jamplayer.MusicListener;
import com.tianscar.jamplayer.MusicPlayer;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class LoopExample {

    private static final AtomicInteger loopsLeft = new AtomicInteger(3);

    public static void main(String[] args) {
        try (MusicPlayer player = new MusicPlayer()) {
            System.out.println("Loop " + loopsLeft.get() + " times, total " + (loopsLeft.get() + 1));
            player.addMusicListener(new MusicListener() {
                @Override
                public void update(MusicEvent event) {
                    if (event.getSource() == player) {
                        if (event.getType() == MusicEvent.Type.STOP) {
                            if (loopsLeft.getAndDecrement() <= 0) return;
                            System.out.println("Loops left: " + loopsLeft.get());
                            try {
                                player.prepare();
                                player.start();
                            } catch (LineUnavailableException | UnsupportedAudioFileException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            });
            player.setDataSource(Thread.currentThread().getContextClassLoader(), "fbodemo1.wav");
            System.out.println("Loops left: " + loopsLeft.get());
            player.prepare();
            player.start();
            while (loopsLeft.get() >= 0) {
                Thread.sleep(1000);
            }
        }
        catch (UnsupportedAudioFileException | LineUnavailableException | IOException | InterruptedException e) {
            throw new RuntimeException("LoopTest failed: ", e);
        }
    }

}
