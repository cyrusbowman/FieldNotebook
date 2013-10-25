package com.openatk.fieldnotebook;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

public class ScrollAutoView extends ScrollView {
	public ScrollAutoView(Context context) {
		super(context);
	}
	public ScrollAutoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public ScrollAutoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if(added == false){
			this.scrollTo(0, scrollY);
			added = true;
		}
	}

	int scrollY = 0;
	Boolean added = true;
	public void scrollToAfterAdd(int where){
		scrollY = where;
		added = false;
		this.scrollTo(0, where);
	}
}
