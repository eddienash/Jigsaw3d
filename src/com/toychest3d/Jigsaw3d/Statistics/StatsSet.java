package com.toychest3d.Jigsaw3d.Statistics;

import java.util.Arrays;

import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.Puzzle.Persistance;
import com.toychest3d.Jigsaw3d.Selector.Selector;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class StatsSet {
	
	public StatsSet(Context context, String modelName, boolean loadFromPersistance) {
		
		mContext = context;
		mModelName = modelName;
		
		mLevels = context.getResources().getStringArray(R.array.sel_difficulties_prompts).length;
		
		mTotalGames = new int[mLevels];
		Arrays.fill(mTotalGames, 0);
		mTotalTime = new int[mLevels];
		Arrays.fill(mTotalTime, 0);
		mBestTime = new int[mLevels];
		Arrays.fill(mBestTime, 0);
		mTotalWins = new int[mLevels];
		Arrays.fill(mTotalWins, 0);
		
		if(loadFromPersistance)
			loadStats();
	}
	
	public void addGame(int level) {
		
		mTotalGames[level]++;
		saveStats();
	}

	public void addWin(int level, int time) {
		
		mTotalWins[level]++;
		mTotalTime[level] += time;
		mBestTime[level] = mBestTime[level] == 0 ? time : (time < mBestTime[level] ? time : mBestTime[level]);
		
		saveStats();
	}
	
	void saveStats() {
		
		String stats = "";
		for(int i=0; i<mLevels; i++) {
			stats += String.valueOf(mTotalGames[i]) + ":";
			stats += String.valueOf(mTotalWins[i]) + ":";
			stats += String.valueOf(mTotalTime[i]) + ":";
			stats += String.valueOf(mBestTime[i]) + (i < (mLevels - 1) ? ":" : "");
		}
		
		Persistance.saveStats(mModelName, stats);
	}

	private void loadStats() {
		
		String savedStats = Persistance.getStats(mModelName);
		
		if(savedStats != null) {
			
			String[] stats = savedStats.split(":");
			int idx = 0;
			for(int i=0; i<mLevels; i++) {
				mTotalGames[i] = Integer.parseInt(stats[idx++]);
				mTotalWins[i] = Integer.parseInt(stats[idx++]);
				mTotalTime[i] = Integer.parseInt(stats[idx++]);
				mBestTime[i] = Integer.parseInt(stats[idx++]);
			}
		}
	}
	
	void show (TableLayout parent) {
		
		// load subtitles
		String[] prompts = mContext.getResources().getStringArray(R.array.sel_difficulties_prompts);
		String[] subTitles = new String[prompts.length];
		for(int i=0; i<prompts.length; i++)
			subTitles[i] = prompts[i].substring(0, prompts[i].indexOf(':'));
		
		TextView title = cloneTextView(mContext, R.id.stat_title_label_template, Selector.prettyName(mModelName));
		parent.addView(title);
		
		for(int i=0; i<subTitles.length; i++) {
			
			TableRow detailRow = new TableRow(mContext);
			parent.addView(detailRow);
			
			detailRow.addView(cloneTextView(mContext, R.id.stat_detail_label_template, subTitles[i]));
			detailRow.addView(cloneTextView(mContext, R.id.stat_detail_played_template, String.valueOf(mTotalGames[i])));
			detailRow.addView(cloneTextView(mContext, R.id.stat_detail_completed_template, String.valueOf(mTotalWins[i])));
			
			if(mTotalWins[i] > 0) {
				int ave = (int)((float)mTotalTime[i] / (float)mTotalWins[i]);
				detailRow.addView(cloneTextView(mContext, R.id.stat_detail_average_template, timeFormat(ave)));
				detailRow.addView(cloneTextView(mContext, R.id.stat_detail_best_template, timeFormat(mBestTime[i])));
			}
		}
	}
	
	private String timeFormat(int seconds) {
		
		int secs = seconds;
		int hrs = secs / 3600; 
		int mins = (secs - hrs * 3600) / 60;
		secs = secs - hrs * 3600 - mins * 60;
		
		if(hrs > 0)
			return String.format("%02d:%02d:%02d", hrs, mins, secs);
		else
			return String.format("%2d:%02d", mins, secs);
	}
	
	private TextView cloneTextView(Context context, int textViewId, String newText) {
		
		TextView srcTextView = (TextView)((Activity)context).findViewById(textViewId);
		TextView newView = new TextView(context);
		
		newView.setPadding(srcTextView.getPaddingLeft(), srcTextView.getPaddingTop(), srcTextView.getPaddingRight(), srcTextView.getPaddingBottom());
		
		
		newView.setTextColor(srcTextView.getTextColors());
		newView.setTypeface(srcTextView.getTypeface());
		newView.setGravity(srcTextView.getGravity());
		newView.setBackgroundDrawable(srcTextView.getBackground());
		newView.setText(newText);

		newView.setLayoutParams((MarginLayoutParams) srcTextView.getLayoutParams());
		return newView;
	}
	
	int[] mTotalGames, mTotalWins, mTotalTime, mBestTime;
	
	private int mLevels;
	private Context mContext;
	private String mModelName;
}