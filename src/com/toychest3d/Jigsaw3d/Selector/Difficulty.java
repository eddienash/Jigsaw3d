package com.toychest3d.Jigsaw3d.Selector;

import java.util.Random;

import android.content.Context;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RadioButton;

class Difficulty extends RadioButton {
	
	Difficulty(Context context, int level, String prompt, RadioButton rdoTemplate) {
		super(context);
		
		this.setText(prompt);
		this.setId(mId++);
		mLevel = level;
		
		// android:layout_width, android:layout_height, android:layout_marginLeft
		MarginLayoutParams lp = (MarginLayoutParams) rdoTemplate.getLayoutParams();
		this.setLayoutParams(lp);
		
        // android:drawablePadding
		int cdp = rdoTemplate.getCompoundDrawablePadding();
		this.setCompoundDrawablePadding(cdp);
		
        // android:paddingLeft
		int pl = rdoTemplate.getPaddingLeft();
		int pr = rdoTemplate.getPaddingRight();
		int pt = rdoTemplate.getPaddingTop();
		int pb = rdoTemplate.getPaddingBottom();
		this.setPadding(pl, pt, pr, pb);
		
		this.setBackgroundDrawable(rdoTemplate.getBackground().getConstantState().newDrawable());
		this.setTextColor(rdoTemplate.getTextColors());
		
		mCuts = mLevels[level];
	}
	
	int[] getCuts(Random rand) {
		
		int diffIndex = (rand.nextInt(mCuts.length/3)) * 3;
		return new int[] {mCuts[diffIndex], mCuts[diffIndex+1], mCuts[diffIndex+2]};
	}
	
	private static int mId = 1;
	private byte[] mCuts;
	int mLevel;
	
//	<item>Really easy: less than 10 pieces</item>
	private static final byte[] reallyEasy = new byte[] {
			0,	0,	1,
			0,	1,	0,
			1,	0,	0,
			0,	0,	2,
			0,	2,	0,
			2,	0,	0,
			0,	0,	3,
			0,	1,	1,
			0,	3,	0,
			1,	0,	1,
			1,	1,	0,
			3,	0,	0,
			0,	1,	2,
			0,	2,	1,
			1,	0,	2,
			1,	2,	0,
			2,	0,	1,
			2,	1,	0,
			0,	1,	3,
			0,	3,	1,
			1,	0,	3,
			1,	1,	1,
			1,	3,	0,
			3,	0,	1,
			3,	1,	0,
			0,	2,	2,
			2,	0,	2,
			2,	2,	0
	};
	
    //<item>Easy: 10 yo 19 pieces</item>
	private static final byte[] easy = new byte[] {
		0,	2,	3,
		0,	3,	2,
		1,	1,	2,
		1,	2,	1,
		2,	0,	3,
		2,	1,	1,
		2,	3,	0,
		3,	0,	2,
		3,	2,	0,
		0,	3,	3,
		1,	1,	3,
		1,	3,	1,
		3,	0,	3,
		3,	1,	1,
		3,	3,	0,
		1,	2,	2,
		2,	1,	2,
		2,	2,	1
	};
	
    //<item>Moderate: 20 to 29 pieces</item>
	private static final byte[] moderate = new byte[] {
		1,	2,	3,
		1,	3,	2,
		2,	1,	3,
		2,	3,	1,
		3,	1,	2,
		3,	2,	1,
		2,	2,	2
	};
	
    //<item>Hard: 30 to 39 pieces</item>
	private static final byte[] hard = new byte[] {
		1,	3,	3,
		3,	1,	3,
		3,	3,	1,
		2,	2,	3,
		2,	3,	2,
		3,	2,	2,
	};
	
    //<item>Really hard: 40 to 49 pieces</item>
	private static final byte[] reallyHard = new byte[] {
		2,	3,	3,
		3,	2,	3,
		3,	3,	2,
		3,	3,	3
	};
	
	private static final byte[][] mLevels = new byte[][] {reallyEasy, easy, moderate, hard, reallyHard};
}
