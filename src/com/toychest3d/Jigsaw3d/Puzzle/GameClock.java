package com.toychest3d.Jigsaw3d.Puzzle;

public class GameClock {
	
	public GameClock() {
		reset();
	}

	public void reset() {

		systemTimeMillis = System.currentTimeMillis();
		mLastDelta = systemTimeMillis;
		mStart = systemTimeMillis;
	}
	
	public void tick() {
		
		// Everything in milliseconds
		systemTimeMillis = System.currentTimeMillis();
		elapsedMilli = systemTimeMillis - mStart;
		deltaMilli = systemTimeMillis - mLastDelta;
		mLastDelta = systemTimeMillis;
		
		// seconds versions
		systemTimeSecs = (float)systemTimeMillis / 1000.0f;
		elapsedSecs = (float)elapsedMilli / 1000.0f;
		deltaSecs = (float)deltaMilli / 1000.0f;
	}
	
	// now
	public long systemTimeMillis;
	public float systemTimeSecs;
	
	// time since last tick or reset
	public long deltaMilli;
	public float deltaSecs;
	
	// time since reset
	public long elapsedMilli;
	public float elapsedSecs;
	
	private long mStart;
	private long mLastDelta;
}
