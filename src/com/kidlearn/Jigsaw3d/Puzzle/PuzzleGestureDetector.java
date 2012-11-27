package com.kidlearn.Jigsaw3d.Puzzle;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

class PuzzleGestureDetector {

	static void initialize(Context context) {
		
		// UI parameters
		mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
		mLongPressSlopSquared = ViewConfiguration.get(context).getScaledTouchSlop() * ViewConfiguration.get(context).getScaledTouchSlop();
		mDoubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
		mDoubleTapSlopSquared = ViewConfiguration.get(context).getScaledDoubleTapSlop() * ViewConfiguration.get(context).getScaledDoubleTapSlop();
		
		mTouchTimer = new Timer("TouchTimer");

		mState = Gstate.IDLE;
		mDownEvent = null;
		mLPsync = new Object();
		mLPstate = LPstate.CANCELED;
	}
	
	static void destroy() {
		mTouchTimer.cancel();		
	}
	
	static OnTouchListener onGlTouch = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			PuzzleGestureDetector.onTouch(GestureEvent.MOTION_EVENT, event);
			return true;
		}
	};
	
	static void onTouch(GestureEvent ge, MotionEvent me) {
		
		if(ge == GestureEvent.MOTION_EVENT) {
			
			int event = me.getActionMasked();
			
			switch(mState) {
			
			case IDLE:
				if(event == MotionEvent.ACTION_DOWN) {
					
					// is this a piece?
					int pieceIdx = PieceTransforms.touchedPiece(me);
					if(pieceIdx < Puzzle.mPieces.size()) {
						mState = Gstate.DOWN;
						StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_PIECE_DOWN, me, pieceIdx);
					}
					
					else {
						
						// is this a double tap?
						boolean doubleTap = mDownEvent == null ? false : (me.getDownTime() - mDownEvent.getDownTime()) < mDoubleTapTimeout;
						if(doubleTap) {
							float dSquared = (me.getX() - mDownEvent.getX()) * (me.getX() - mDownEvent.getX()); 
							dSquared += (me.getY() - mDownEvent.getY()) * (me.getY() - mDownEvent.getY());
							doubleTap = dSquared < mDoubleTapSlopSquared;
						}
						if(doubleTap) {
							StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_DOUBLE_TAP, me);
							mDownEvent = null;
						}
						
						// possible long press
						else {
							mLongPressTask = new TimerTask() {
								@Override
								public void run() {
									synchronized(mLPsync) {
										if(mLPstate == LPstate.SCHEDULED) {
											((Activity)PuzzleActivity.mContext).runOnUiThread(mPostLongPress);
											mLPstate = LPstate.QUEUED_TO_GUI;
										}
									}
								}
							};
							synchronized(mLPsync) {
								mTouchTimer.schedule(mLongPressTask, mLongPressTimeout);
								mLPstate = LPstate.SCHEDULED;
							}
							mDownEvent = MotionEvent.obtain(me);
							mState = Gstate.LP_MAYBE;
						}
					}
				}
				break;
				
			case LP_MAYBE:
				if((event == MotionEvent.ACTION_CANCEL) || (event == MotionEvent.ACTION_UP)) {
					
					synchronized(mLPsync) {
						if(mLPstate == LPstate.POSTED) {
							StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_UP_CANCEL, me);
						}
						mLongPressTask.cancel();
						mLPstate = LPstate.CANCELED;
					}
					mState = Gstate.IDLE;
				}
				
				else if(event == MotionEvent.ACTION_MOVE) {
					
					// distance moved since DOWN
					float dSquared = (me.getX() - mDownEvent.getX()) * (me.getX() - mDownEvent.getX()); 
					dSquared += (me.getY() - mDownEvent.getY()) * (me.getY() - mDownEvent.getY());
					
					if(dSquared > mLongPressSlopSquared) {
						synchronized(mLPsync) {
							mLongPressTask.cancel();
							mLPstate = LPstate.CANCELED;
						}
						StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_WORLD_DOWN, mDownEvent);
						StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_MOVE, me);
						mDownEvent = null;
						mState = Gstate.DOWN;
					}
				}
				break;
				
			case DOWN:
				if((event == MotionEvent.ACTION_CANCEL) || (event == MotionEvent.ACTION_UP)) {
					StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_UP_CANCEL, me);
					mState = Gstate.IDLE;
				}
				else if(event == MotionEvent.ACTION_MOVE) {
					StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_MOVE, me);
				}
				break;
			}
		}
		
		else if(ge == GestureEvent.LONG_PRESS) {
			synchronized(mLPsync) {
				if(mLPstate == LPstate.QUEUED_TO_GUI) {
					StateMachine2.postEvent(StateMachine2.Gevent.GESTURE_LONG_PRESS, mDownEvent);
					mLPstate = LPstate.POSTED;
					mState = Gstate.DOWN;
				}
			}
		}
	}
	
	private static Runnable mPostLongPress = new Runnable() {
		public void run() {
			onTouch(GestureEvent.LONG_PRESS, null);
		}
	};
	
	private static Object mLPsync;
	private enum LPstate {SCHEDULED, QUEUED_TO_GUI, POSTED,CANCELED};
	private static LPstate mLPstate;

	private enum Gstate {IDLE, LP_MAYBE, DOWN};
	private enum GestureEvent {MOTION_EVENT, LONG_PRESS};
	
	private static TimerTask mLongPressTask;
	private static Gstate mState;
	private static Timer mTouchTimer;
	private static long mLongPressTimeout, mDoubleTapTimeout;
	private static float mLongPressSlopSquared, mDoubleTapSlopSquared;
	private static MotionEvent mDownEvent;
}