package com.tianscar.jamplayer.test;

import com.tianscar.jamplayer.MusicPlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class PlayAllTest {

    private void play(String format) throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play(format, null);
    }

    private void play(String format, String container) throws UnsupportedAudioFileException, LineUnavailableException,
            IOException, InterruptedException {
        MusicPlayer player = new MusicPlayer();
        player.setDataSource(Thread.currentThread().getContextClassLoader(), "fbodemo1" +
                (container == null ? ("." + format) : ("_" + format + ".") + container));
        player.prepare();
        player.start();
        Assertions.assertNotEquals(player.getMicrosecondLength(), AudioSystem.NOT_SPECIFIED);
        System.out.println("duration (seconds): " + player.getMicrosecondLength() / 1_000_000.0);
        Thread.sleep(player.getMicrosecondLength() / 1_000L);
    }

    @Test
    @DisplayName("Play WAV")
    public void playWAV() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("wav");
    }

    @Test
    @DisplayName("Play AIFF")
    public void playAIFF() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("aiff");
    }

    @Test
    @DisplayName("Play AIFC")
    public void playAIFC() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("aifc");
    }

    @Test
    @DisplayName("Play AU/SND")
    public void playAUAKASND() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("au");
    }

    @Test
    @DisplayName("Play FLAC")
    public void playFLAC() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("flac");
    }

    @Test
    @DisplayName("Play MP4 ALAC")
    public void playMP4ALAC() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("alac", "m4a");
    }

    @Test
    @DisplayName("Play AAC")
    public void playAAC() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("aac");
    }

    @Test
    @DisplayName("Play MP4 AAC")
    public void playMP4AAC() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("aac", "m4a");
    }

    @Test
    @DisplayName("Play Shorten")
    public void playShorten() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("shn");
    }

    @Test
    @DisplayName("Play WavPack")
    public void playWavPack() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("wv");
    }

    @Test
    @DisplayName("Play MP2")
    public void playMP2() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("mp2");
    }

    @Test
    @DisplayName("Play MP3")
    public void playMP3() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("mp3");
    }

    @Test
    @DisplayName("Play APE/MAC")
    public void playAPEAKAMAC() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("ape");
    }

    @Test
    @DisplayName("Play Ogg Speex")
    public void playOggSpeex() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("speex", "ogg");
    }

    @Test
    @DisplayName("Play Ogg Vorbis")
    public void playOggVorbis() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        play("vorbis", "ogg");
    }

}
