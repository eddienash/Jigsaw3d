package com.toychest3d.Jigsaw3d.Welcome;

import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.Help.HelpActivity;
import com.toychest3d.Jigsaw3d.Installer.ZipInstaller;
import com.toychest3d.Jigsaw3d.Preferences.PreferencesActivity;
import com.toychest3d.Jigsaw3d.Puzzle.Persistance;
import com.toychest3d.Jigsaw3d.Puzzle.PuzzleActivity;
import com.toychest3d.Jigsaw3d.Selector.Selector;
import com.toychest3d.Jigsaw3d.Selector.SelectorActivity;
import com.toychest3d.Jigsaw3d.Statistics.StatisticsActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WelcomeActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.welcome);
		
		mContext = this;
		
    	findViewById(R.id.btnNewPuzzle).setOnClickListener(mOnButtonClicks);
    	findViewById(R.id.btnResumePuzzle).setOnClickListener(mOnButtonClicks);
    	findViewById(R.id.btnHelp).setOnClickListener(mOnButtonClicks);
    	findViewById(R.id.btnPreference).setOnClickListener(mOnButtonClicks);
    	findViewById(R.id.btnStatistics).setOnClickListener(mOnButtonClicks);
    	
    	Persistance.initialize(this);
		if(!Persistance.getInstalled())
			Persistance.setInstalled(ZipInstaller.installIncluded(this));
		
		mLayout = findViewById(R.id.layoutWelcome);
		Persistance.setMenuBackdrop(mLayout);
	}
	
	@Override
	public void onResume() {
		
		Button resume = (Button)findViewById(R.id.btnResumePuzzle);
		String btnText = getResources().getString(R.string.btn_welcome_resume);
		String modelName = Persistance.getModelName();
		
		if(modelName != null) {
			resume.setEnabled(true);
			btnText += " \"" + Selector.prettyName(modelName) + "\"";
		}
		else
			resume.setEnabled(false);

		resume.setText(btnText);

    	super.onResume();
	}
	
	private OnClickListener mOnButtonClicks = new OnClickListener() {
		public void onClick(View arg0) {
			
			Intent intent = null;
			
			switch (arg0.getId()) {
			
			case R.id.btnNewPuzzle:
				intent = new Intent(mContext, SelectorActivity.class);
				break;

			case R.id.btnResumePuzzle:
				intent = new Intent(mContext, PuzzleActivity.class);
				break;
			
			case R.id.btnHelp:
				intent = new Intent(mContext, HelpActivity.class);
				break;
			
			case R.id.btnPreference:
				intent = new Intent(mContext, PreferencesActivity.class);
				break;
			
			case R.id.btnStatistics:
				intent = new Intent(mContext, StatisticsActivity.class);
				break;
			
			}
			
			if(intent != null) {
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mContext.startActivity(intent);
			}
		}
	};
	
	private Context mContext;
	public static View mLayout;
}
