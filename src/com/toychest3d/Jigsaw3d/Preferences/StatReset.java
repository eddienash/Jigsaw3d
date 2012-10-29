package com.toychest3d.Jigsaw3d.Preferences;

import com.toychest3d.Jigsaw3d.Statistics.StatisticsActivity;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class StatReset extends DialogPreference {

	public StatReset(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if(positiveResult) {
			StatisticsActivity.zeroStats(mContext);
		}
	}
	
	private Context mContext;
}
