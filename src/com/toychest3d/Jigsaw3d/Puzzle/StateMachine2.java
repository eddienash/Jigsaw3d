package com.toychest3d.Jigsaw3d.Puzzle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ToggleButton;

import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.Statistics.StatsSet;

class StateMachine2 {

	static void initialize(Context context) {
		mContext = context;

		mGameTimer = new GameTimer(1000);
		mState = null;
		enterState(Persistance.getPuzzleState());
	}
	
	// must be called on the gui thread
	static void postEvent(Gevent ge) {
		postEvent(ge, null, 0);
	}
	static void postEvent(Gevent ge, MotionEvent me) {
		postEvent(ge, me, 0);
	}
	static void postEvent(Gevent ge, MotionEvent me, int pieceIdx) {
		
		//Log.d("nash","event: " + ge.name());

		switch (ge) {

		case ZOOM_IN:
			WorldTransforms.zoomIn();
			break;
		case ZOOM_OUT:
			WorldTransforms.zoomOut();
			break;
		case SET_PAN_MODE:
			((ToggleButton)((Activity)mContext).findViewById(R.id.btnPan)).setChecked(true);
			((ToggleButton)((Activity)mContext).findViewById(R.id.btnRotate)).setChecked(false);
			WorldTransforms.mMode = WorldTransforms.Mode.PAN;
			break;
		case SET_ROTATE_MODE:
			((ToggleButton)((Activity)mContext).findViewById(R.id.btnPan)).setChecked(false);
			((ToggleButton)((Activity)mContext).findViewById(R.id.btnRotate)).setChecked(true);
			WorldTransforms.mMode = WorldTransforms.Mode.ROTATE;
			break;
		case START:
			Blaster.initialize(mContext);
			Blaster.start();
			if(Persistance.mSounds)
				PuzzleActivity.mSoundPool.play(PuzzleActivity.mExplodeSound, 1, 1, 5, 0, 1);
			enterState(State.EXPLODING);
			break;
		case RESUME:
			mGameTimer.start(mGameTimer.mElaspedSecs);
			enterState(State.PLAYING);
			break;
		case WIN:
			new StatsSet(mContext, Persistance.getModelName(), true).addWin(Persistance.getDifficulty(), (int)mGameTimer.getElapsedSecs());
			Persistance.save();
			AlertDialog alert = new AlertDialog.Builder(mContext).create();
			alert.setMessage(mContext.getString(R.string.dlg_winner));
			alert.setButton(AlertDialog.BUTTON_POSITIVE, "OK",  new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}});
			alert.show();
			if(Persistance.mSounds)
				PuzzleActivity.mSoundPool.play(PuzzleActivity.mWinSound, 1, 1, 5, 0, 1);
			enterState(State.WON);
			break;
		case RENDER_READY:
			PuzzleActivity.mDlgLoading.dismiss();
			switch (mState) {
			case IDLE:
			case WAIT_FOR_START:
			case EXPLODING:
				enterState(State.WAIT_FOR_START);
				break;
			case PEEKING:
			case WAIT_FOR_RESUME:
			case PLAYING:
				mGameTimer.mElaspedSecs = Persistance.getGameSeconds();
				for (Piece piece : Puzzle.mPieces)
					Persistance.getTranslates(piece);
				Puzzle.updateHomeCount();
				WorldTransforms.setRotatedView(Persistance.getZoom(), Persistance.getRotateX(), Persistance.getRotateY(),
																Persistance.getPanX(), Persistance.getPanY());
				enterState(State.WAIT_FOR_RESUME);
				break;
			case WON:
				enterState(State.WON);
				break;
			}
			break;
		case BLASTER_COMPLETE:
			new StatsSet(mContext, Persistance.getModelName(), true).addGame(Persistance.getDifficulty());
			Puzzle.updateHomeCount();
			mGameTimer.start(0);
			enterState(State.PLAYING);
			break;
		case PAUSE:
			PuzzleGestureDetector.destroy();
			Blaster.pause();
			mGameTimer.pause();
			break;
			
		case GESTURE_DOUBLE_TAP:
			if(mState == State.PLAYING) {
				WorldTransforms.fit();
				enterState(State.PLAYING);
			}
			break;
		case GESTURE_LONG_PRESS:
			if(mState == State.PLAYING) {
				WorldTransforms.saveRotatedView();
				WorldTransforms.rotateEvent(me);
	   			enterState(State.PEEKING);
			}
			break;
		case GESTURE_UP_CANCEL:
			if(mState == State.PEEKING) {
				WorldTransforms.restoreRotatedView();
				enterState(State.PLAYING);
			}
			else if(mState == State.PIECE) {
				PieceTransforms.onTouch(mTouchedPiece, me);
				mTouchedPiece.mTouched = false;
				enterState(State.PLAYING);
			}
			else if(mState == State.WORLD) {
				enterState(State.PLAYING);
			}
			Puzzle.updateHomeCount();
			break;
		case GESTURE_WORLD_DOWN:
			switch (mState) {
			case WON:
			case WAIT_FOR_START:
				WorldTransforms.rotateEvent(me);
				break;
			case PLAYING:
				WorldTransforms.rotateEvent(me);
				enterState(State.WORLD);
			}
			break;
		case GESTURE_PIECE_DOWN:
			switch (mState) {
			case WAIT_FOR_START:
			case WON:
				WorldTransforms.rotateEvent(me);
				break;
			case PLAYING:
				mTouchedPiece = Puzzle.mPieces.get(pieceIdx);
				mTouchedPiece.mTouched = true;
				PieceTransforms.onTouch(mTouchedPiece, me);
				enterState(State.PIECE);
				break;
			}
			break;
		case GESTURE_MOVE:
			switch (mState) {
			case WORLD:
			case WON:
			case PEEKING:
			case WAIT_FOR_START:
				WorldTransforms.rotateEvent(me);
				break;
			case PIECE:
				PieceTransforms.onTouch(mTouchedPiece, me);
			}
			break;
		}
		
		PuzzleActivity.mGl.requestRender();
	}
	
	private static void enterState(State state) {
		
		//Log.d("nash","entering state: " + state.name());
		
		if(state == mState)
			return;
		
		((Activity)mContext).findViewById(R.id.btnStart).setVisibility(View.GONE);
		((Activity)mContext).findViewById(R.id.btnResume).setVisibility(View.GONE);

		((Activity)mContext).findViewById(R.id.layoutZoom).setVisibility(View.GONE); // zoomIn/Out, Pan/Rotate buttons
		((Activity)mContext).findViewById(R.id.layoutWon).setVisibility(View.GONE); // won new, more, stats, quit
		
		// set up gui
		switch (state) {
		
		case IDLE:
			break;
		case WAIT_FOR_START:
			((Activity)mContext).findViewById(R.id.btnStart).setVisibility(View.VISIBLE);
			Puzzle.mRenderMode = Puzzle.RenderMode.ONE;
			break;
		case WAIT_FOR_RESUME:
			((Activity)mContext).findViewById(R.id.btnResume).setVisibility(View.VISIBLE);
			Puzzle.mRenderMode = Puzzle.RenderMode.ONE;
			break;
		case EXPLODING:
			Puzzle.mRenderMode = Puzzle.RenderMode.ALL;
			break;
		case WORLD:
		case PIECE:
		case PLAYING:
			((Activity)mContext).findViewById(R.id.layoutZoom).setVisibility(View.VISIBLE);
			Puzzle.mRenderMode = Puzzle.RenderMode.ALL;
			break;
		case WON:
			Puzzle.mRenderMode = Puzzle.RenderMode.ALL;
			((Activity)mContext).findViewById(R.id.layoutWon).setVisibility(View.VISIBLE);
			break;
		case PEEKING:
			Puzzle.mRenderMode = Puzzle.RenderMode.ONE;
			break;
		}
		
		mState = state;
	}
	
	enum Gevent {
		ZOOM_IN, ZOOM_OUT, SET_PAN_MODE, SET_ROTATE_MODE, START, RESUME, RENDER_READY, BLASTER_COMPLETE, WIN, PAUSE,
		GESTURE_PIECE_DOWN, GESTURE_WORLD_DOWN, GESTURE_DOUBLE_TAP, GESTURE_UP_CANCEL, GESTURE_LONG_PRESS, GESTURE_MOVE
	};
	enum State {IDLE, WAIT_FOR_START, WAIT_FOR_RESUME, EXPLODING, PLAYING, WON, PEEKING, WORLD, PIECE};
	static State mState;
	static GameTimer mGameTimer;
	
	private static Context mContext;
	private static Piece mTouchedPiece;
}