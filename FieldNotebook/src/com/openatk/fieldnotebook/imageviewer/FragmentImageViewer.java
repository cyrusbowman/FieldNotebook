package com.openatk.fieldnotebook.imageviewer;

import java.util.List;

import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.MainActivity.DropDownAnim;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Image;
import com.openatk.fieldnotebook.notelist.FragmentNoteList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class FragmentImageViewer extends Fragment implements OnClickListener, OnTouchListener {
	private static final String TAG = FragmentImageViewer.class.getSimpleName();

	
	private ImageView ivImage;
	private ImageButton butDone;
	
	private ImageViewerListener listener;
	
	private Boolean initialCreate;
	private List<Image> images;
	private Image currentImage = null;
	private Integer currentImageIndex = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState == null){
			initialCreate = true;
		} else {
			initialCreate = false;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_image_viewer, container, false);
		
		butDone = (ImageButton) view.findViewById(R.id.fragment_image_viewer_done);
		ivImage = (ImageView) view.findViewById(R.id.fragment_image_viewer_image);

		ivImage.setOnTouchListener(this);
		
		butDone.setOnClickListener(this);
		ivImage.setOnClickListener(this);

		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Fragment parentFragment = getParentFragment();
		if (parentFragment != null && parentFragment instanceof ImageViewerListener) {
			// Check if parent fragment (if there is one) is listener
			listener = (ImageViewerListener) parentFragment;
		} else if (activity != null && activity instanceof ImageViewerListener) {
			// Otherwise, check if parent activity is the listener
			listener = (ImageViewerListener) activity;
		} else {
			throw new ClassCastException(activity.toString() + " must implement FragmentImageViewer.ImageViewerListener");
		}
		Log.d(TAG, "Attached");
	}	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener.ImageViewerRequestData(this);
	}

	public void populateData(List<Image> images, Image clicked) {
		Log.d(TAG, "Populate Data");
		this.images = images;
		this.currentImage = clicked;
		for(int i=0; i<images.size(); i++){
			if(this.currentImage == images.get(i)){
				this.currentImageIndex = i;
			}
		}
		
		Bitmap bitmap = BitmapFactory.decodeFile(this.currentImage.getPath());
		Drawable d = new BitmapDrawable(this.getResources(), bitmap);
		
		this.ivImage.setImageDrawable(d);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.fragment_image_viewer_done) {
			this.ivImage.setVisibility(View.GONE);
			this.listener.ImageViewerDone(this.currentImage);
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float eventY = event.getRawY();
		float eventX = event.getRawX();
		
		switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
            	this.DragDown(eventX);
               break; 
            }
            case MotionEvent.ACTION_UP:
            {     
            	 this.DragUp(eventX);
                 break;
            }
            case MotionEvent.ACTION_MOVE:
            {
            	this.DragDragging(eventX);
                break;
            }
        }
        return true;
	}
	
	private float startDrag = 0;
	private void DragDown(float start) {
		startDrag = start;
	}

	private void DragDragging(float whereX) {
		this.ivImage.setTranslationX(whereX - startDrag);
	}
	
	private void DragUp(float whereX) {
		if(Math.abs(whereX - startDrag) > 10.0f){
			//Switch images
			Image newImage = null;
			if((whereX - startDrag) > 0){
				//Prev
				if(this.currentImageIndex != 0){
					this.currentImageIndex = this.currentImageIndex - 1;
					newImage = this.images.get(this.currentImageIndex);
				}
			} else {
				//Next
				if(this.currentImageIndex != (this.images.size() - 1)){

					this.currentImageIndex = this.currentImageIndex + 1;
					newImage = this.images.get(this.currentImageIndex);
				}
			}
			if(newImage != null){
				this.currentImage = newImage;
				Bitmap bitmap = BitmapFactory.decodeFile(this.currentImage.getPath());
				Drawable d = new BitmapDrawable(this.getResources(), bitmap);
				this.ivImage.setImageDrawable(d);
			}
		}
		this.ivImage.clearAnimation();
		this.ivImage.setTranslationX(0.0f);
	}
}