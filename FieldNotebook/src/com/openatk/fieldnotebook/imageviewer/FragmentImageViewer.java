package com.openatk.fieldnotebook.imageviewer;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.db.Image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.ImageButton;


public class FragmentImageViewer extends Fragment implements OnClickListener {
	private static final String TAG = FragmentImageViewer.class.getSimpleName();

	
	private ImageButton butDone;
	
	private ImageViewerListener listener;
	
	private Boolean initialCreate;
	private List<Drawable> drawables = new ArrayList<Drawable>();
	private List<Image> images = new ArrayList<Image>();
	
	private Image currentImage = null;
	private Integer currentImageIndex = null;
	ViewPager mViewPager = null;
	
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
		
		mViewPager = (HackyViewPager) view.findViewById(R.id.fragment_image_viewer_view_pager);
		butDone = (ImageButton) view.findViewById(R.id.fragment_image_viewer_done);
		mViewPager.setVisibility(View.GONE);
		mViewPager.setOnClickListener(this);
		butDone.setOnClickListener(this);
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

	public void populateData(List<Image> images, Image clicked, int size) {
		Log.d(TAG, "Populate Data");
		this.currentImage = clicked;
		this.images = images;
		
		if(size != 0){
			//Fullscreen
			this.getView().setBackgroundColor(Color.argb(220, 0, 0, 0));
		}
		
		for(int i=0; i<this.images.size(); i++){
			Bitmap bitmap = BitmapFactory.decodeFile(this.images.get(i).getPath());
			Drawable d = new BitmapDrawable(getResources(), bitmap);
			drawables.add(d);
			if(this.currentImage == this.images.get(i)){
				this.currentImageIndex = i;
			}
		}
		mViewPager.setAdapter(new SamplePagerAdapter());
		mViewPager.setCurrentItem(this.currentImageIndex);
		mViewPager.setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.fragment_image_viewer_done) {
			this.listener.ImageViewerDone(this.currentImage);
		} else if(v == this.mViewPager){
			int item = mViewPager.getCurrentItem();
			listener.ImageViewerClick(images.get(item));
		}
	}
	
	class SamplePagerAdapter extends PagerAdapter implements OnPhotoTapListener {
		@Override
		public int getCount() {
			return drawables.size();
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			PhotoView photoView = new PhotoView(container.getContext());
			photoView.setImageDrawable(drawables.get(position));
			photoView.setOnPhotoTapListener(this);
			// Now just add PhotoView to ViewPager and return it
			container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			return photoView;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public void onPhotoTap(View view, float x, float y) {
			int item = mViewPager.getCurrentItem();
			listener.ImageViewerClick(images.get(item));
		}

	}
}