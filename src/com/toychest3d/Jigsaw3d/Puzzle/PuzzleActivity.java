package com.toychest3d.Jigsaw3d.Puzzle;

import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.Help.HelpActivity;
import com.toychest3d.Jigsaw3d.Preferences.PreferencesActivity;
import com.toychest3d.Jigsaw3d.Puzzle.StateMachine2.Gevent;
import com.toychest3d.Jigsaw3d.Selector.Selector;
import com.toychest3d.Jigsaw3d.Selector.SelectorActivity;
import com.toychest3d.Jigsaw3d.Statistics.StatisticsActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class PuzzleActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = this;
		
		// TODO this returns false if not supported. Then what??
		getWindow().requestFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.puzzle);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.puzzle_title);
		String title = (String) getApplicationInfo().loadLabel(getPackageManager());
		String modelName = Persistance.getModelName();
		title += modelName != null ? (" - " + Selector.prettyName(modelName)) : "";
		((TextView)findViewById(R.id.txtPuzzleTitle)).setText(title);
		mTxtPuzzleClock = ((TextView)findViewById(R.id.txtPuzzleClock));
		mTxtPuzzleScore = ((TextView)findViewById(R.id.txtPuzzleScore));
		
		mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		mPopSound = mSoundPool.load(this, R.raw.gum_bubble_pop, 0);
		mWinSound = mSoundPool.load(this, R.raw.short_triumphal_fanfare, 0);
		mExplodeSound = mSoundPool.load(this, R.raw.dacha_polka, 0);
		
		initGui();

		mGl = (GLSurfaceView) findViewById(R.id.glPuzzleSurface);
		mGl.setRenderer(new PlayRenderer(this));
		mGl.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		StateMachine2.initialize(this);
		PuzzleGestureDetector.initialize(this);
		mGl.setOnTouchListener(PuzzleGestureDetector.onGlTouch);
		
		mDlgLoading = new ProgressDialog(this);
		mDlgLoading.setIndeterminate(true);
		mDlgLoading.setMessage(this.getString(R.string.msg_loading));
		mDlgLoading.setCancelable(true);
		mDlgLoading.show();
		
		// force a render because on return from screensaver may not get one
		PlayRenderer.mRendered = false;
		mGl.requestRender();
		mGl.setSoundEffectsEnabled(Persistance.mSounds);
	}

	@Override
	public void onPause() {
		super.onPause();
		StateMachine2.postEvent(StateMachine2.Gevent.PAUSE);
		Persistance.save();
		mSoundPool.stop(mPopSound);
		mSoundPool.stop(mWinSound);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.puzzle_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true; // TODO place holder
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		Intent intent = null;
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.mnuRestart:
			StateMachine2.mState = StateMachine2.State.IDLE;
			Persistance.save();
			intent = this.getIntent();
			break;

		case R.id.mnuHelp:
			intent = new Intent(this, HelpActivity.class);
			break;

		case R.id.mnuPrefs:
			intent =  new Intent(this, PreferencesActivity.class);
			break;

		default:
			return super.onOptionsItemSelected(item);
		}
		
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		return true;
	}
	
	private void initGui() {
		
		findViewById(R.id.btnZoomIn).setOnClickListener(mGuiListener);
		findViewById(R.id.btnZoomOut).setOnClickListener(mGuiListener);
		findViewById(R.id.btnRotate).setOnClickListener(mGuiListener);
		findViewById(R.id.btnPan).setOnClickListener(mGuiListener);
		findViewById(R.id.btnStart).setOnClickListener(mGuiListener);
		findViewById(R.id.btnResume).setOnClickListener(mGuiListener);
		findViewById(R.id.btnWonGetNew).setOnClickListener(mGuiListener);
		findViewById(R.id.btnWonGetMore).setOnClickListener(mGuiListener);
		findViewById(R.id.btnWonStats).setOnClickListener(mGuiListener);
		findViewById(R.id.btnWonQuit).setOnClickListener(mGuiListener);
		
		((ToggleButton)((Activity)mContext).findViewById(R.id.btnPan)).setChecked(false);
		((ToggleButton)((Activity)mContext).findViewById(R.id.btnRotate)).setChecked(true);
		WorldTransforms.mMode = WorldTransforms.Mode.ROTATE;
	}
	
	private OnClickListener mGuiListener = new OnClickListener() {
		public void onClick(View v) {
			
			Intent intent = null;

			switch (v.getId()) {
			case R.id.btnZoomIn: StateMachine2.postEvent(Gevent.ZOOM_IN); break;
			case R.id.btnZoomOut: StateMachine2.postEvent(Gevent.ZOOM_OUT); break;
			case R.id.btnRotate: StateMachine2.postEvent(Gevent.SET_ROTATE_MODE); break;
			case R.id.btnPan: StateMachine2.postEvent(Gevent.SET_PAN_MODE); break;
			case R.id.btnStart: StateMachine2.postEvent(Gevent.START); break;
			case R.id.btnResume: StateMachine2.postEvent(Gevent.RESUME); break;
			
			case R.id.btnWonGetNew:
				intent = new Intent(mContext, SelectorActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mContext.startActivity(intent);
				((Activity)PuzzleActivity.mContext).finish();
				break;
			case R.id.btnWonGetMore:
				break;
			case R.id.btnWonStats:
				intent = new Intent(mContext, StatisticsActivity.class);
				// TODO: - I dont know why this causes the stats activity to only launch the first time
				//intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mContext.startActivity(intent);
				break;
			case R.id.btnWonQuit:
				((Activity)mContext).finish();
				break;
			}
		}
	};

	static TextView mTxtPuzzleClock, mTxtPuzzleScore;
	static GLSurfaceView mGl;
	static Context mContext;
	static Menu mMenu;
	static ProgressDialog mDlgLoading;
	static SoundPool mSoundPool;
	static int mPopSound, mWinSound, mExplodeSound;
}
