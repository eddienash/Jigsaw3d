package com.kidlearn.Jigsaw3d.Puzzle;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;


class GameTimer {
	
	GameTimer(long periodMS) {
		
		mPeriodMS = periodMS;
		mGtimer = null;
		mGameClock = new GameClock();
		mTimerTask = null;
	}
	
	void start(float elapsedSecs) {
		
		mElaspedSecs = elapsedSecs;
		mGameClock.reset();
		
		mTimerTask = new TimerTask() {
			@Override
			public void run() {
				((Activity)PuzzleActivity.mContext).runOnUiThread(mClockUpdate);
			}
		};
		mGtimer = new Timer();
		mGtimer.schedule(mTimerTask, 0, mPeriodMS);
	}
	
	void pause() {
		if(mGtimer != null)
			mGtimer.cancel();
		
		if(mTimerTask != null)
			mTimerTask.cancel();
	}
	
	Runnable mClockUpdate = new Runnable() {
		public void run() {

			mGameClock.tick();
			updateDisplay();

			// winner
			if((Puzzle.mNumPieces > 0) && (Puzzle.mHomeCount == Puzzle.mNumPieces)) {
				pause();
				((Activity)PuzzleActivity.mContext).runOnUiThread(new Runnable() {
					public void run() {
						StateMachine2.postEvent(StateMachine2.Gevent.WIN);
					}
				});
			}
		}
	};
	
	void updateDisplay() {
		
		int secs = (int) (mGameClock.elapsedSecs + mElaspedSecs);
		int hrs = secs / 3600; 
		int mins = (secs - hrs * 3600) / 60;
		secs = secs - hrs * 3600 - mins * 60;
		
		PuzzleActivity.mTxtPuzzleClock.setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
		
		PuzzleActivity.mTxtPuzzleScore.setText(String.format("%d / %d", Puzzle.mHomeCount, Puzzle.mNumPieces));
	}
	
	float getElapsedSecs() {
		return mGameClock.elapsedSecs + mElaspedSecs;
	}

	private Timer mGtimer;
	private TimerTask mTimerTask;
	private GameClock mGameClock;
	private long mPeriodMS;
	float mElaspedSecs;
}
