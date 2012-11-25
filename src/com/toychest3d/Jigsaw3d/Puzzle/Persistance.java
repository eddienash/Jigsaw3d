package com.toychest3d.Jigsaw3d.Puzzle;

import com.toychest3d.Jigsaw3d.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.view.View;

public class Persistance {
	
	public static void initialize(Context context) {

		mContext = context;
		
		mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
		mEdit = mSharedPreference.edit();
		PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
		loadUserPrefs(context);
	}
	
	// ************************************ SAVES ***************************************

	static void save() {

		// puzzle details
		mEdit.putString("PuzzleState", StateMachine2.mState.name());
		mEdit.putFloat("GameClock", StateMachine2.mGameTimer.getElapsedSecs());
		
		// piece transforms
		float[] translates = new float[3 * Puzzle.mPieces.size()];
		PieceTransforms.getTransforms(translates);
		for (Piece piece : Puzzle.mPieces) {
			mEdit.putFloat("piece" + piece.mIndex + "translate0", translates[piece.mIndex * 3]);
			mEdit.putFloat("piece" + piece.mIndex + "translate1", translates[(piece.mIndex * 3) + 1]);
			mEdit.putFloat("piece" + piece.mIndex + "translate2", translates[(piece.mIndex * 3) + 2]);
		}
		
		// world transforms
		mEdit.putFloat("zoom", WorldTransforms.mZoom);
		mEdit.putFloat("rotatex", WorldTransforms.mRotateX);
		mEdit.putFloat("rotatey", WorldTransforms.mRotateY);
		mEdit.putFloat("panx", WorldTransforms.mPanX);
		mEdit.putFloat("pany", WorldTransforms.mPanY);
		
		mEdit.commit();
	}
	
	public static void saveNewPuzzle(String modelName, int[] cutsPerAxis, int diffLevel) {
		mEdit.putString("PuzzleState", StateMachine2.State.IDLE.name());
		mEdit.putString("ModelName", modelName);
		mEdit.putInt("MeshCutsX", cutsPerAxis[0]);
		mEdit.putInt("MeshCutsY", cutsPerAxis[1]);
		mEdit.putInt("MeshCutsZ", cutsPerAxis[2]);
		mEdit.putInt("diffLevel", diffLevel);
		mEdit.commit();
	}
	
	public static void setStateNew() {
		mEdit.putString("PuzzleState", StateMachine2.State.IDLE.name());
		mEdit.commit();
	}
	
	public static void setInstalled(boolean installed) {
		mEdit.putBoolean("InstallFlag", installed);
		mEdit.commit();
	}
	
	public static void saveStats(String modelName, String stats) {
		mEdit.putString("Stats_" + modelName, stats);
		mEdit.commit();
	}
	
	// ************************************ Game State GETS ****************************************
	
	public static String getStats(String modelName) {return mSharedPreference.getString("Stats_" + modelName, null);}

	public static boolean getInstalled() {return mSharedPreference.getBoolean("InstallFlag", false);}
	
	public static StateMachine2.State getPuzzleState()  {return StateMachine2.State.valueOf(mSharedPreference.getString("PuzzleState", "IDLE"));}
	
	public static String getModelName() {return mSharedPreference.getString("ModelName", null);}
	
	public static int[] getMeshCuts() {
		return new int[] {mSharedPreference.getInt("MeshCutsX", 0), mSharedPreference.getInt("MeshCutsY", 0), mSharedPreference.getInt("MeshCutsZ", 0)};
	}
	
	static float getGameSeconds() {return mSharedPreference.getFloat("GameClock", 0);}
	
	static void getTranslates(Piece piece) {
		int idx = piece.mIndex;
		PieceTransforms.setPieceTranslate(idx, mSharedPreference.getFloat("piece" + idx + "translate0", 0),
				mSharedPreference.getFloat("piece" + idx + "translate1", 0), mSharedPreference.getFloat("piece" + idx + "translate2", 0));
	}
	
	static float getZoom() {return mSharedPreference.getFloat("zoom", 1);}
	
	static float getPanX() {return mSharedPreference.getFloat("panx", 0);}
	
	static float getPanY() {return mSharedPreference.getFloat("pany", 0);}
	
	static float getRotateX() {return mSharedPreference.getFloat("rotatex", 0);}
	
	static float getRotateY() {return mSharedPreference.getFloat("rotatey", 0);}
	
	public static int getDifficulty() {return mSharedPreference.getInt("diffLevel", 0);}

	// ************************************ User Preference FIELDS ****************************************
	// these are fields for performance see res/xml/preferences.xml for values
	
	public static void loadUserPrefs(Context context) {

		// all defaults set in xml so null should never be returned
		mHint = mSharedPreference.getString(context.getString(R.string.prefs_hint_key), null).equals("dots") ? HINT.Dots : HINT.Boxes;
		
		mSounds = mSharedPreference.getBoolean(context.getString(R.string.prefs_sound_key), false);
		
		mBackdrop = mSharedPreference.getString(context.getString(R.string.prefs_backdrops_key), null);

		return;
	}
	
	public static void setMenuBackdrop(View v) {
		
		try {
			Bitmap bmp = BitmapFactory.decodeStream(mContext.getAssets().open("backdrops/" + mBackdrop));
			BitmapDrawable bd = new BitmapDrawable(mContext.getResources(), bmp);
			bd.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			v.setBackgroundDrawable(bd);
		}
		catch (Exception e) {}
	}
	
	enum HINT {Dots, Boxes};
	static HINT mHint;
	static boolean mSounds;
	public static String mBackdrop;
	private static SharedPreferences mSharedPreference;
	private static SharedPreferences.Editor mEdit;
	private static Context mContext;
}
