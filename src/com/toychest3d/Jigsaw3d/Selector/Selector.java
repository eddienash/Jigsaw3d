package com.toychest3d.Jigsaw3d.Selector;

import java.io.IOException;

import com.toychest3d.Jigsaw3d.Installer.ZipInstaller;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RadioButton;

public class Selector extends RadioButton {

	Selector(Context context, String modelName, RadioButton rdoTemplate) {
		super(context);
		
		mModelName = modelName;
		mContext = context;
		
		this.setText(prettyName(mModelName));
		this.setId(mId++);
		
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
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		
		try {
			Drawable icon = new BitmapDrawable(mContext.getResources(), ZipInstaller.textureInputStream(mContext, mModelName, "thumb.png"));
			icon.setBounds(new Rect(1,1,h,h));
			this.setCompoundDrawables(icon, null, null, null);
		} catch (IOException e) {}
	}
	
	public static String prettyName(String name) {
		
		String pretty = "";
		char[] chars = name.toCharArray();
		
		boolean firstChar = true;
		
		for (char c : chars) {
			
			if( c == '_') {
				pretty += " ";
				firstChar = true;
			}

			else if(firstChar) {
				pretty += Character.toUpperCase(c);
				firstChar = false;
			}
			
			else
				pretty += c;
		}
		
		return pretty;
	}
	
	private static int mId = 1;
	String mModelName;
	private Context mContext;
}
