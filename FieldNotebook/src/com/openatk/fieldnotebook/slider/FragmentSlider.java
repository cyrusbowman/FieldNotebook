package com.openatk.fieldnotebook.slider;

import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.MainActivity.DropDownAnim;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.fieldlist.FragmentFieldList;
import com.openatk.fieldnotebook.notelist.FragmentNoteList;
import com.openatk.fieldnotebook.sidebar.FragmentSidebar;

import android.app.Activity;
import android.graphics.Point;
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
import android.widget.RelativeLayout;
import android.widget.TextView;


public class FragmentSlider extends Fragment implements OnClickListener, OnTouchListener {
	private static final String TAG = FragmentSlider.class.getSimpleName();

	
	private TextView tvName;
	private TextView tvAcres;
	private ImageButton butEditField;
	private Button butAddNote;
	private ImageButton butBackToFields;
	private ViewGroup fieldMenu;
	private ViewGroup noteMenu;
	
	private SliderListener listener;
	private Field currentField = null;
	private View container = null;
	
	
	private Boolean initialCreate;
	private ViewGroup noteListContainer;
	private ViewGroup fieldListContainer;
	FragmentNoteList fragmentNoteList;
	FragmentFieldList fragmentFieldList;

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
		View view = inflater.inflate(R.layout.fragment_slider, container, false);

		tvName = (TextView) view.findViewById(R.id.sidebar_tvName);
		tvAcres = (TextView) view.findViewById(R.id.sidebar_tvAcres);
		
		//view.setOnTouchListener(this);
		//tvName.setOnTouchListener(this);
		//tvAcres.setOnTouchListener(this);
		
		butEditField = (ImageButton) view.findViewById(R.id.sidebar_butEditField);
		butAddNote = (Button) view.findViewById(R.id.sidebar_butAddNote);
		butBackToFields = (ImageButton) view.findViewById(R.id.sidebar_butBackToFields);
		fieldMenu = (ViewGroup) view.findViewById(R.id.sidebar_layMenuFields);
		noteMenu = (ViewGroup) view.findViewById(R.id.sidebar_layMenuNotes);
		fieldListContainer = (ViewGroup) view.findViewById(R.id.slider_fragment_listFields_container);
		noteListContainer = (ViewGroup) view.findViewById(R.id.slider_fragment_listNotes_container);

		butEditField.setOnClickListener(this);
		butAddNote.setOnClickListener(this);
		butBackToFields.setOnClickListener(this);
				
		// If this is the first creation of the fragment, add child fragments
		if (initialCreate) {
			initialCreate = false;
			// Prepare a transaction to add fragments to this fragment
			FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

			// Add the list fragment to this fragment's layout
			if (fieldListContainer != null) {
				Log.i(TAG, "onCreate: adding FragmentFieldList to FragmentSidebar");
				// Add the fragment to the this fragment's container layout
				fragmentFieldList = new FragmentFieldList();
				fragmentTransaction.replace(fieldListContainer.getId(), fragmentFieldList, FragmentFieldList.class.getName());
			}
			if (noteListContainer != null) {
				Log.i(TAG, "onCreate: adding FragmentNoteList to FragmentSidebar");
				// Add the fragment to the this fragment's container layout
				fragmentNoteList = new FragmentNoteList();
				fragmentTransaction.replace(noteListContainer.getId(), fragmentNoteList, FragmentNoteList.class.getName());
			}
			// Commit the transaction
			fragmentTransaction.commit();
		}
				
		// If this is the first creation of the fragment, add child fragments
		if (initialCreate) {
			initialCreate = false;
			// Prepare a transaction to add fragments to this fragment
			FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

			// Add the list fragment to this fragment's layout
			noteListContainer = (ViewGroup) view.findViewById(R.id.slider_fragment_listNotes_container);
			if (noteListContainer != null) {
				Log.i(TAG, "onCreate: adding FragmentNoteList to FragmentSlider");

				// Add the fragment to the this fragment's container layout
				fragmentNoteList = new FragmentNoteList();
				fragmentTransaction.replace(noteListContainer.getId(), fragmentNoteList, FragmentNoteList.class.getName());
			}
			// Commit the transaction
			fragmentTransaction.commit();
		}
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Fragment parentFragment = getParentFragment();
		if (parentFragment != null && parentFragment instanceof SliderListener) {
			// Check if parent fragment (if there is one) is listener
			listener = (SliderListener) parentFragment;
		} else if (activity != null && activity instanceof SliderListener) {
			// Otherwise, check if parent activity is the listener
			listener = (SliderListener) activity;
		} else {
			throw new ClassCastException(activity.toString() + " must implement FragmentSlider.SliderListener");
		}
		Log.d("FragmentSlider", "Attached");
	}	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener.SliderRequestData(this);
	}

	public void populateData(Field theField, View container) {
		Log.d("FragmentSidebar", "Populate Data");
		//Get current field
		currentField = theField;
		this.container = container;
		if (currentField != null) {
			tvName.setText(currentField.getName());
			tvAcres.setText(Integer.toString(currentField.getAcres()) + " ac");
			fieldListContainer.setVisibility(View.GONE);
			fieldMenu.setVisibility(View.GONE);
			noteListContainer.setVisibility(View.VISIBLE);
			noteMenu.setVisibility(View.VISIBLE);
		} else {
			tvName.setText("");
			tvAcres.setText("");
			fieldListContainer.setVisibility(View.VISIBLE);
			fieldMenu.setVisibility(View.VISIBLE);
			noteListContainer.setVisibility(View.GONE);
			noteMenu.setVisibility(View.GONE);
		}
	}

	public int getHeight() {
		// Method so close transition can work
		return getView().getHeight();
	}
	
	public int oneNoteHeight() {
		//TODO return get from FragmentNoteList
		return 0;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.sidebar_butEditField) {
			listener.SliderEditField();
		} else if (v.getId() == R.id.sidebar_butAddField) {
			listener.SliderAddField();
		} else if (v.getId() == R.id.sidebar_butBackToFields){
			listener.SliderBackToFieldsList();
		} else if (v.getId() == R.id.sidebar_butAddNote){
			listener.SliderAddNote();
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float eventY = event.getRawY();
		
		switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
            	this.SliderDragDown((int)eventY);
               break; 
            }
            case MotionEvent.ACTION_UP:
            {     
            	 this.SliderDragUp((int)(eventY));
                 break;
            }
            case MotionEvent.ACTION_MOVE:
            {
            	this.SliderDragDragging((int)(eventY));
                break;
            }
        }
        return true;
	}
	
	private int sliderStartDrag = 0;
	private int sliderHeightStart = 0;
	private void SliderDragDown(int start) {
		if(container != null){
			int height = container.getHeight();
			FrameLayout layout = (FrameLayout) this.getView().findViewById(R.id.slider_fragment_listNotes_container);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			sliderStartDrag = height - start - params.height;
			sliderHeightStart = params.height;
		}
	}

	private void SliderDragDragging(int whereY) {
		if(container != null){
			int height = container.getHeight();
		
			FrameLayout layout = (FrameLayout) this.getView().findViewById(R.id.slider_fragment_listNotes_container);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			
			if((height - whereY - sliderStartDrag) > 0){
				params.height = height - whereY - sliderStartDrag;
			} else {
				params.height = 0;
			}
			layout.setLayoutParams(params);
		}
	}
	
	private void SliderDragUp(int whereY) {
		//Slider done dragging snap to 1 of 3 positions
		if(container != null){
			int oneThirdHeight = container.getHeight() / 3;
			if(whereY < oneThirdHeight){
				//Fullscreen
				Log.d("SliderDragUp", "fullscreen");
			} else if(whereY < oneThirdHeight * 2) {
				//Middle
				Log.d("SliderDragUp", "middle");
	
			} else {
				//Closed
				Log.d("SliderDragUp", "closed");
			}
			//Find end height
			FrameLayout layout = (FrameLayout) this.getView().findViewById(R.id.slider_fragment_listNotes_container);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			if(params.height > sliderHeightStart){
				//Make bigger
				SliderGrow();
			} else {
				//Make smaller
				SliderShrink();
			}
		}
	}
	
	private int sliderPosition = 0;
	private void SliderShrink(){
		if(container != null){
			int oneThirdHeight = container.getHeight() / 3;
			FrameLayout layout = (FrameLayout) this.getView().findViewById(R.id.slider_fragment_listNotes_container);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			if(sliderPosition == 2 || sliderPosition == 1){
				//Middle -> Small
				//OneNote -> Small
				DropDownAnim an = new DropDownAnim(layout, params.height, 0);
				an.setDuration(300);
				layout.startAnimation(an);
				sliderPosition = 0;
			} else if(sliderPosition == 3){
				//Fullscreen -> Middle if has notes
				//Fullscreen -> Small if no notes
				if(this.fragmentNoteList != null && this.fragmentNoteList.hasNotes()){
					DropDownAnim an = new DropDownAnim(layout, params.height, oneThirdHeight);
					an.setDuration(300);
					layout.startAnimation(an);
					sliderPosition = 2;
				} else {
					DropDownAnim an = new DropDownAnim(layout, params.height, 0);
					an.setDuration(300);
					layout.startAnimation(an);
					sliderPosition = 0;
				}
			}
			layout.setLayoutParams(params);
		}
	}
	private void SliderGrow(){
		if(container != null){
			int oneThirdHeight = container.getHeight() / 3;	
			RelativeLayout relAdd = (RelativeLayout) this.getView().findViewById(R.id.slider_layMenu);
			Log.d("layMenu:", Integer.toString(relAdd.getHeight()));
			FrameLayout layout = (FrameLayout) this.getView().findViewById(R.id.slider_fragment_listNotes_container);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			if(sliderPosition == 0 || sliderPosition == 1){
				//Small -> Middle
				//OneNote -> Middle
				DropDownAnim an = new DropDownAnim(layout, params.height, oneThirdHeight);
				an.setDuration(300);
				layout.startAnimation(an);
				sliderPosition = 2;
			} else if(sliderPosition == 2){
				//Middle -> Fullscreen
				DropDownAnim an = new DropDownAnim(layout, params.height, (container.getHeight() - relAdd.getHeight()));
				an.setDuration(300);
				layout.startAnimation(an);
				sliderPosition = 3;
			}
			layout.setLayoutParams(params);
		}
	}
	private void SliderOneNote(){
		RelativeLayout relAdd = (RelativeLayout) this.getView().findViewById(R.id.slider_layMenu);
		Log.d("layMenu:", Integer.toString(relAdd.getHeight()));
		FrameLayout layout = (FrameLayout) this.getView().findViewById(R.id.slider_fragment_listNotes_container);
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
		
		DropDownAnim an = new DropDownAnim(layout, params.height, this.oneNoteHeight());
		an.setDuration(300);
		layout.startAnimation(an);
		sliderPosition = 1;
		
		layout.setLayoutParams(params);
	}
	public void SliderSizeMiddle(){
		if(sliderPosition == 3){
			this.SliderShrink();
		} else if(sliderPosition == 0){
			this.SliderGrow();
		}
	}
	
	private class DropDownAnim extends Animation {
	    int targetHeight;
	    int startHeight;
	    View view;

	    public DropDownAnim(View view, int startHeight, int targetHeight) {
	        this.view = view;
	        this.startHeight = startHeight;
	        this.targetHeight = targetHeight;
	    }

	    @Override
	    protected void applyTransformation(float interpolatedTime, Transformation t) {
	        int newHeight = (int) (startHeight - ((startHeight - targetHeight) * interpolatedTime));
	        view.getLayoutParams().height = newHeight;
	        view.requestLayout();
	    }

	    @Override
	    public void initialize(int width, int height, int parentWidth,
	            int parentHeight) {
	        super.initialize(width, height, parentWidth, parentHeight);
	    }

	    @Override
	    public boolean willChangeBounds() {
	        return true;
	    }
	}
	
}