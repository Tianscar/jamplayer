package com.tianscar.jamplayer.test;

import javax.sound.sampled.LineUnavailableException;

import com.tianscar.jamplayer.SoundClip;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SoundClipTest {

	public static final int DEFAULT_BUFFER_FRAMES = 1024;
	public static final int SOUND_VOLUME_STEPS = 1024;
	public static final int SOUND_SPEED_STEPS = 4096;
	
	@Test
	public void testBasicProperties() {
		float[] pcmData = new float[44100];
		SoundClip testClip = new SoundClip(pcmData, 2);
		
		// stereo pcm array created, so assume 2 pcm values per frame
		Assertions.assertEquals(44100/2, testClip.getFrameLength());
		Assertions.assertEquals(500_000, testClip.getMicrosecondLength());
		
		testClip.close();
	}
	
	@Test
	public void testPolyphony() {
		float[] pcmData = new float[44100];
		SoundClip testClip = new SoundClip(pcmData, 2);
		
		int instance0 = testClip.obtainInstance();
		int instance1 = testClip.obtainInstance();
		int instance2 = testClip.obtainInstance();
		
		// Are instance IDs in the expected range?
		Assertions.assertTrue(instance0 >= 0 && instance0 < 2);
		Assertions.assertTrue(instance1 >= 0 && instance1 < 2);
		// At this point there should not be any instances available.
		Assertions.assertEquals(-1, instance2);

		testClip.close();
	}
	
	// @TODO test state logic (active, running)
	
	@Test
	public void testCursorPositioning() {
		// data will be one second in duration, 44100 total frames
		float[] pcmData = new float[88200]; 
		SoundClip testClip = new SoundClip(pcmData, 2);
		
		int instance0 = testClip.obtainInstance();
		
		Assertions.assertEquals(44100, testClip.getFrameLength());

		/*
		testClip.setFractionalPosition(instance0, 0);
		Assertions.assertEquals(0, testClip.getFramePosition(instance0));
		testClip.setFractionalPosition(instance0, 0.5);
		Assertions.assertEquals(22050, testClip.getFramePosition(instance0));
		testClip.setFractionalPosition(instance0, 1);
		Assertions.assertEquals(44100, testClip.getFramePosition(instance0));
		 */
		
		testClip.seekToMicroseconds(instance0, 0);
		Assertions.assertEquals(0, testClip.getFramePosition(instance0));
		testClip.seekToMicroseconds(instance0, 500_000);
		Assertions.assertEquals(22050, testClip.getFramePosition(instance0));
		testClip.seekToMicroseconds(instance0, 1_000_000);
		Assertions.assertEquals(44100, testClip.getFramePosition(instance0));
		
		// Testing exceptions
		// When instance0 is released, isActive will be set to false.
		// Attempts to setFractionalPosition should now throw exception.
		testClip.releaseInstance(instance0);
		Exception thrown = Assertions.assertThrows(
				IllegalStateException.class, () -> {
					testClip.seekToFrames(instance0, 0);
				});		
		Assertions.assertEquals(
				"instance: " + instance0 + " is inactive", 
				thrown.getMessage());
		
		thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
					testClip.seekToMicroseconds(instance0, 0);
				});		
		Assertions.assertEquals(
				"instance: " + instance0 + " is inactive", 
				thrown.getMessage());

		// When an instance is started, isPlaying == true
		// which should throw exception.
		int instance1 = testClip.obtainInstance();
		testClip.start(instance1);
		Assertions.assertTrue(testClip.isPlaying(instance1));
		thrown = Assertions.assertThrows(
				IllegalStateException.class, () -> {
					testClip.seekToFrames(instance1, 0);
				});		
		Assertions.assertEquals(
				"You need to call the function before instance: " + instance1 + " playing",
				thrown.getMessage());
		
		thrown = Assertions.assertThrows(
				IllegalStateException.class, () -> {
					testClip.seekToMicroseconds(instance1, 0);
				});		
		Assertions.assertEquals(
				"You need to call the function before instance: " + instance1 + " playing",
				thrown.getMessage());

		testClip.close();
	}
	
	@Test
	public void testGetPcmCopy() {
		int clipLen = 128;
		float[] testClipData = new float[clipLen];
		// fill with random signed, normalized floats
		for (int i = 0; i < clipLen; i++) {
			testClipData[i] = (float) Math.random() * 2 - 1;
		}
		SoundClip testClip = new SoundClip(testClipData, 1);
		float[] pcmCopy = testClip.copyPCM();
		Assertions.assertEquals(clipLen, pcmCopy.length);
		
		for (int i = 0; i < clipLen; i++) {
			Assertions.assertEquals(pcmCopy[i], testClipData[i]);
		}

		testClip.close();
	}
	
	@Test
	public void testVolumeBasics() {

		float[] pcmData = new float[100]; 
		SoundClip testClip = new SoundClip(pcmData, 5);
		
		// default volume
		int instance0 = testClip.play();
		Assertions.assertEquals(1, testClip.getLeftVolume(instance0));

		// Volume changes incrementally while playing the clip.
		// Since we aren't actually running the "playing" 
		// methods, the change should not have taken effect.		
		testClip.setVolume(instance0, 0.25);
		Assertions.assertNotEquals(0.25, testClip.getLeftVolume(instance0));
		
		testClip.stop(instance0);
		testClip.setVolume(instance0, 0.75);
		// isPlaying == false, so setVolume() should take immediate effect
		Assertions.assertEquals(0.75, testClip.getLeftVolume(instance0));
		
		// Play, with volume specified
		int instance1 = testClip.play(0.5);
		Assertions.assertEquals(0.5, testClip.getLeftVolume(instance1));
		int instance2 = testClip.play(0.75, 0, 1, 0);
		Assertions.assertEquals(0.75, testClip.getLeftVolume(instance2));
		
		// test clamps
		int instance3 = testClip.play(1.5);
		Assertions.assertEquals(1, testClip.getLeftVolume(instance3));
		int instance4 = testClip.play(-1);
		Assertions.assertEquals(0, testClip.getLeftVolume(instance4));

		testClip.close();
	}

	@Test
	public void testSpeedBasics() {

		float[] pcmData = new float[100]; 
		SoundClip testClip = new SoundClip(pcmData, 4);
		
		// default speed
		int instance0 = testClip.play();
		Assertions.assertEquals(1, testClip.getSpeed(instance0));

		// Speed changes incrementally while playing the clip.
		// Since we aren't actually running the "playing" 
		// methods, the change should not have taken effect.
		testClip.setSpeed(instance0, 2.5);
		Assertions.assertNotEquals(2.5, testClip.getSpeed(instance0));
		
		testClip.stop(instance0);
		testClip.setSpeed(instance0, 0.75);
		// isPlaying == false, so setSpeed() should take immediate effect
		Assertions.assertEquals(0.75, testClip.getSpeed(instance0));
		
		// play(), with speed specified
		int instance1 = testClip.play(0.5, -0.5, 3, 0);
		Assertions.assertEquals(3, testClip.getSpeed(instance1));

		// test clamps
		int instance2 = testClip.play(0.25, 0.25, 9, 0);
		Assertions.assertEquals(8, testClip.getSpeed(instance2));
		int instance3 = testClip.play(0.25, -1.5, 0.1, 0);
		Assertions.assertEquals(0.125, testClip.getSpeed(instance3));
		testClip.close();
	}

	@Test
	public void testReadBasics() {
		/*
		 * Do we need to mock the data in clip[]? This isn't a 
		 * case where we are interacting with a different class.
		 * Establish some credibility by loading the SoundClip
		 * with a preset array, and then seeing if the buffer
		 * returned by read matches.
		 */
		
		float[] clipData = new float[DEFAULT_BUFFER_FRAMES * 2];
		float lastFrame = DEFAULT_BUFFER_FRAMES - 1;
		// values range from 0 to 1 over the buffer.
		for(int i = 0, n = DEFAULT_BUFFER_FRAMES; i < n; i++) {
			clipData[i * 2] = i / lastFrame;
			clipData[(i * 2) + 1] = clipData[i * 2];
		}

		SoundClip testClip = new SoundClip(clipData, 1);
		
		// This sets variable needed for reading track (cursor.isPlaying) but 
		// does not output to SDL because we haven't opened the SoundClip.
		testClip.play();
		
		// This makes use of the default readBuffer instantiated in the
		// SoundClip constructor.
		float[] testBuffer = new float[DEFAULT_BUFFER_FRAMES * 2];
		testClip.read(testBuffer);
		
//		for (int i = 0, n = testBuffer.length; i < n; i++) {
//			System.out.println("i:" + i + "\tval:" + testBuffer[i] + "\tclip:" + clipData[i]);
//		}
		
		Assertions.assertArrayEquals(clipData, testBuffer);

		testClip.close();
	}
	
	@Test
	public void testDynamicVolume() {
		
		float[] clipData = new float[SOUND_VOLUME_STEPS * 2];
		int lastFrame = clipData.length - 2;
		// Goal is to show the output volume goes from the initial
		// val to the target volume over the course of VOLUME_STEPS
		// Test data is given a stable value so the output buffer
		// should just reflect the volume factor changing from 
		// 1 to 0.5.
		for(int i = 0, n = clipData.length; i < n; i++) {
			clipData[i] = 0.8f;
		}

		SoundClip testClip = new SoundClip(clipData, 1);
				
		// Default play() method sets volume to initial value of 1f.
		int instance0 = testClip.play();
		// When playing, volume changes are spread out over DEFAULT_SOUND_VOLUME_STEPS.
		double targetLeftVolume = 0.5;
		testClip.setVolume(instance0, targetLeftVolume);
		// Set to loop so that instance0 does not recycle, so that we 
		// can execute .getLeftVolume() on instance0 at end of test.
		testClip.setLooping(instance0, -1);

		float[] testBuffer = new float[DEFAULT_BUFFER_FRAMES * 2];
		testClip.read(testBuffer);
		
		for (int i = 0, n = testBuffer.length - 2; i < n; i+=2) {
			// The PCM out values should progressively diminish as the progressively lowers. 
			Assertions.assertTrue(clipData[i] - testBuffer[i] < clipData[i + 2] - testBuffer[i + 2]);
		}
		// Expect the cursor's volume to have reached the target volume.
		Assertions.assertEquals(targetLeftVolume, testClip.getLeftVolume(instance0));

		testClip.close();
	}
		
	@Test 
	public void testSpeedOutput() {
		/*
		 * For the test data, using PCM that goes up from 0
		 * by increments of 0.0001. This will make it easier
		 * to see if the LERP is working correctly on non-integer
		 * frame positions.
		 */
		float[] clipData = new float[DEFAULT_BUFFER_FRAMES * 2];
		for(int i = 0; i < DEFAULT_BUFFER_FRAMES; i++) {
			clipData[i * 2] = i * 0.0001f;
			clipData[i * 2 + 1] = clipData[i * 2];
		}

		SoundClip testClip = new SoundClip(clipData, 1);
		
		int instance0 = testClip.obtainInstance();
		testClip.setVolume(instance0, 1);

		// testing 3/4 speed
		float testSpeed = 0.75f;
		testClip.setSpeed(instance0, testSpeed);
		testClip.start(instance0);
		float[] testBuffer = new float[DEFAULT_BUFFER_FRAMES * 2];
		testClip.read(testBuffer);
		
		// Check the current cursor position is at expected
		Assertions.assertEquals(testSpeed * DEFAULT_BUFFER_FRAMES, 
				testClip.getFramePosition(instance0));
		
		// Calculate the expected value after 5 steps taken (i.e., after 5th frame output).
		int testSteps = 5;

		// Test to within 6 decimals (covers float discrepancies) 
		// Why 6? IDK. What is a good amount to use for today's computers?
		float zeroDelta = (float)Math.pow(10, -6);	
		float testCursor = testSpeed * testSteps;
		// LERP formula calculation
		int byteIdx = (int)testCursor * 2;
		float expectedVal = clipData[byteIdx + 2] * (testCursor - (int)testCursor) 
				+ clipData[byteIdx] * (((int)testCursor + 1) - testCursor);
		
		Assertions.assertEquals(expectedVal, testBuffer[testSteps * 2], zeroDelta);

		testClip.close();
	}
	
	@Test
	public void testDynamicSpeed() {

		float[] clipData = new float[SOUND_SPEED_STEPS * 2];
		for(int i = 0; i < SOUND_SPEED_STEPS; i++) {
			clipData[i * 2] = i;
			clipData[i * 2 + 1] = clipData[i * 2];
		}
		
		SoundClip testClip = new SoundClip(clipData, 1);
		
		int instance0 = testClip.obtainInstance();
		testClip.setVolume(instance0, 1);
		testClip.setLooping(instance0, -1);
		
		// The following maneuver is used to set a smaller buffer size, giving us more 
		// opportunities to check the contents of the SoundClipCursor.
		try {
			testClip.open(DEFAULT_BUFFER_FRAMES / 16);
			testClip.close();
		} catch (IllegalStateException | LineUnavailableException e) {
			e.printStackTrace();
		}
		
		testClip.start(instance0);
		
		// Default speed is 1.
		// By setting a new speed after clip has started
		// the speed change will be handled incrementally.
		testClip.setSpeed(instance0, 0.5);
	
		float[] testBuffer = new float[DEFAULT_BUFFER_FRAMES * 2];
		
		double startSpeed = 1;
		double targetSpeed = 0.5;
		double oneIncrement = (targetSpeed - startSpeed) / SOUND_SPEED_STEPS;
		
		int elapsedFrames = 0;
		do {
			testClip.read(testBuffer);
			elapsedFrames += testBuffer.length / 2;
			if (elapsedFrames > SOUND_SPEED_STEPS) break;
			
			// Calculate the expected position of the SoundClipCursor.
			double currFrame = elapsedFrames * startSpeed  + 
					(((elapsedFrames + 1) * elapsedFrames * oneIncrement) / 2.0) ;
			float zeroDelta = (float)Math.pow(10, -6);	
			Assertions.assertEquals(currFrame, testClip.getFramePosition(instance0), zeroDelta);
			
		} while (true);
		Assertions.assertEquals(targetSpeed, testClip.getSpeed(instance0));
	}	
	
}
