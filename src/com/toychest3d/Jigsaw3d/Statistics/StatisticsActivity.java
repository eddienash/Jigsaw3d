package com.toychest3d.Jigsaw3d.Statistics;

import com.toychest3d.Jigsaw3d.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.TableLayout;

public class StatisticsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.statistics);
		
		loadStats(this);

		TableLayout parent = (TableLayout) findViewById(R.id.stat_table_layout);
		mTotals.show(parent);

		for (int i = 0; i < mStatSets.length; i++)
			mStatSets[i].show(parent);

		// remove the template
		parent.removeView(findViewById(R.id.stat_detail_template));
	}
	
	private static void loadStats(Context context) {
		
		mTotals = new StatsSet(context, context.getResources().getString(R.string.stat_label_title), false);

		String[] models = context.fileList();
		mStatSets = new StatsSet[models.length];

		for (int i = 0; i < models.length; i++) {

			int ext = models[i].indexOf(".mesh");
			if (ext > 0) {

				models[i] = models[i].substring(0, ext);

				mStatSets[i] = new StatsSet(context, models[i], true);

				for (int level = 0; level < mTotals.mBestTime.length; level++) {
					if (mStatSets[i].mTotalGames[level] > 0) {
						mTotals.mTotalGames[level] += mStatSets[i].mTotalGames[level];
						mTotals.mTotalWins[level] += mStatSets[i].mTotalWins[level];
						mTotals.mTotalTime[level] += mStatSets[i].mTotalTime[level];
						mTotals.mBestTime[level] = mTotals.mBestTime[level] == 0 ? mStatSets[i].mBestTime[level]
								: (mStatSets[i].mBestTime[level] < mTotals.mBestTime[level] ? mStatSets[i].mBestTime[level] : mTotals.mBestTime[level]);
					}
				}
			}
		}
	}
	
	public static void zeroStats(Context context) {
		
		loadStats(context);

		// zero everything
		for (int level = 0; level < mTotals.mBestTime.length; level++) {

			mTotals.mBestTime[level] = 0;
			mTotals.mTotalGames[level] = 0;
			mTotals.mTotalTime[level] = 0;
			mTotals.mTotalWins[level] = 0;
			
			for (StatsSet stat : mStatSets) {
				stat.mBestTime[level] = 0;
				stat.mTotalGames[level] = 0;
				stat.mTotalTime[level] = 0;
				stat.mTotalWins[level] = 0;
			}
		}

		// write back
		for (int i = 0; i < mStatSets.length; i++)
			mStatSets[i].saveStats();
	}

	private static StatsSet[] mStatSets;
	private static StatsSet mTotals;
}
