package com.kidlearn.Jigsaw3d.Selector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import com.kidlearn.Jigsaw3d.Installer.DogTag;
import com.kidlearn.Jigsaw3d.Installer.ZipInstaller;
import com.kidlearn.Jigsaw3d.Puzzle.Persistance;
import com.kidlearn.Jigsaw3d.Puzzle.PuzzleActivity;
import com.kidlearn.Jigsaw3d.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SelectorActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.selector);
		
		mContext = this;

		mModelSelected = null;
		mSelectorsGroup = (RadioGroup) findViewById(R.id.groupSelectors);
		addSelectors();
		
		mDifficultiesGroup = (RadioGroup) findViewById(R.id.groupDifficulties);
		addDifficulties();
		findViewById(R.id.layoutDifficulty).setVisibility(View.GONE);
		
		mLayoutMain = findViewById(R.id.layoutSelectorRoot);
		Persistance.setMenuBackdrop(mLayoutMain);
		mLayoutSel = findViewById(R.id.groupSelectors);
		Persistance.setMenuBackdrop(mLayoutSel);
		mLayoutDif = findViewById(R.id.groupDifficulties);
		Persistance.setMenuBackdrop(mLayoutDif);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		findViewById(R.id.layoutDifficulty).setVisibility(View.GONE);
		findViewById(R.id.layoutSelector).setVisibility(View.VISIBLE);
		((TextView)findViewById(R.id.txtSelTitle)).setText(R.string.sel_model_prompt);

		// for debugging, reset random onResume()
		mRandom = new Random();
	}

	@Override
	public void onBackPressed() {
		
		if (findViewById(R.id.layoutDifficulty).getVisibility() == View.VISIBLE) {
			findViewById(R.id.layoutDifficulty).setVisibility(View.GONE);
			findViewById(R.id.layoutSelector).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.txtSelTitle)).setText(R.string.sel_model_prompt);
		}
		
		else
			super.onBackPressed();
	}

	private OnClickListener modelSelected = new OnClickListener() {
		public void onClick(View v) {
			mModelSelected = (Selector)v;
			findViewById(R.id.layoutDifficulty).setVisibility(View.VISIBLE);
			findViewById(R.id.layoutSelector).setVisibility(View.GONE);
			((TextView)findViewById(R.id.txtSelTitle)).setText(R.string.sel_difficulty_prompt);
		}
	};
	
	private OnClickListener difficultySelected = new OnClickListener() {
		public void onClick(View v) {
			Persistance.saveNewPuzzle(mModelSelected.mModelName, ((Difficulty)v).getCuts(mRandom), ((Difficulty)v).mLevel);
			Intent intent = new Intent(mContext, PuzzleActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			mContext.startActivity(intent);
			finish();
		}
	};
	
	private void addDifficulties() {

		RadioButton rdoTemplate = (RadioButton) findViewById(R.id.rdoDifficultyTemplate);
		mDifficultiesGroup.removeAllViews();
		
		String[] prompts = getResources().getStringArray(R.array.sel_difficulties_prompts);
		
		for (int i=0; i<prompts.length; i++) {
			
			Difficulty diff = new Difficulty(this, i, prompts[i], rdoTemplate);
			diff.setChecked(Persistance.getDifficulty() == i);
			diff.setOnClickListener(difficultySelected);
			
			mDifficultiesGroup.addView(diff);
		}
	}

	private void addSelectors() {

		RadioButton rdoTemplate = (RadioButton) findViewById(R.id.rdoSelectorTemplate);
		mSelectorsGroup.removeAllViews();
		
		// find every mesh file - and put in alphabetical order
		String[] models = fileList();
		Arrays.sort(models);
		
		for(String model : models) {
			
			int ext = model.indexOf(".mesh");
			if(ext > 0) {
				String modelName = model.substring(0, ext);

				try {
					// license check
					byte[] dogTag = ZipInstaller.getDogTag(this, modelName);
					if(DogTag.check(this, dogTag)) {

						Selector sel = new Selector(mContext, modelName, rdoTemplate);
						
						sel.setChecked(Persistance.getModelName().equals(modelName));
						mModelSelected = sel.isChecked() ? sel : mModelSelected;
						sel.setOnClickListener(modelSelected);

						mSelectorsGroup.addView(sel);
					}
				}
				catch (IOException e) {
				}
			}
		}
	}
	
	static void cloneRadioButtonAttributes(RadioButton button, RadioButton template) {
		
		// android:layout_width, android:layout_height, android:layout_marginLeft
		MarginLayoutParams lp = (MarginLayoutParams) template.getLayoutParams();
		button.setLayoutParams(lp);
		
        // android:drawablePadding
		int cdp = template.getCompoundDrawablePadding();
		button.setCompoundDrawablePadding(cdp);
		
        // android:paddingLeft
		int pl = template.getPaddingLeft();
		int pr = template.getPaddingRight();
		int pt = template.getPaddingTop();
		int pb = template.getPaddingBottom();
		button.setPadding(pl, pt, pr, pb);
		
		button.setBackgroundDrawable(template.getBackground().getConstantState().newDrawable());
		button.setTextColor(template.getTextColors());
		
		
		button.setTextSize(TypedValue.COMPLEX_UNIT_PX, template.getTextSize());
		button.setTextScaleX(template.getTextScaleX());
		button.setTypeface(template.getTypeface());
	}

	private RadioGroup mSelectorsGroup, mDifficultiesGroup;
	private Context mContext;
	private Selector mModelSelected;
	private Random mRandom;
	public static View mLayoutMain, mLayoutSel, mLayoutDif;
}
