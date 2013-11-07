package com.openatk.fieldnotebook.slider;

import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openatk.fieldnotebook.FragmentDrawing;
import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.ScrollAutoView;
import com.openatk.fieldnotebook.FragmentDrawing.DrawingListener;
import com.openatk.fieldnotebook.R.drawable;
import com.openatk.fieldnotebook.R.id;
import com.openatk.fieldnotebook.R.layout;
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Note;
import com.openatk.fieldnotebook.db.TableNotes;
import com.openatk.fieldnotebook.drawing.MyMarker;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolyline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class FragmentSlider extends Fragment implements OnClickListener, OnTouchListener {
	private FragmentSlider me = null;
	private TextView tvName;
	private TextView tvAcres;
	private ImageButton butEditField;
	private Button butShowElevation;
	private Button butShowSoilType;
	private Button butAddNote;
	
	private SliderListener listener;
	private Field currentField = null;
	
	LayoutInflater vi;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_slider, container,
				false);

		me = this;
		tvName = (TextView) view.findViewById(R.id.slider_tvName);
		tvAcres = (TextView) view.findViewById(R.id.slider_tvAcres);
		
		view.setOnTouchListener(this);
		tvName.setOnTouchListener(this);
		tvAcres.setOnTouchListener(this);
		
		butEditField = (ImageButton) view.findViewById(R.id.slider_butEditField);
		butShowElevation = (Button) view.findViewById(R.id.slider_butShowElevation);
		butShowSoilType = (Button) view.findViewById(R.id.slider_butShowSoilType);
		butAddNote = (Button) view.findViewById(R.id.slider_butAddNote);


		butEditField.setOnClickListener(this);
		butShowElevation.setOnClickListener(this);
		butShowSoilType.setOnClickListener(this);
		butAddNote.setOnClickListener(this);
		
		vi = (LayoutInflater) this.getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener.SliderRequestData(this);
	}

	public void populateData(Integer currentFieldId, GoogleMap map) {
		//Get current field
		currentField = null;
		
		//TODO populate data on FragemntNoteList, and request back data for current field
		
		if (currentField != null) {
			tvName.setText(currentField.getName());
			tvAcres.setText(Integer.toString(currentField.getAcres()) + " ac");
		} else {
			tvName.setText("");
			tvAcres.setText("");
		}
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof SliderListener) {
			listener = (SliderListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement FragmentSlider.SliderListener");
		}
		Log.d("FragmentSlider", "Attached");
	}
	
	public int getHeight() {
		// Method so close transition can work
		return getView().getHeight();
	}
	
	public int oneNoteHeight() {
		//TODO return get from FragmentNoteList
		return 0;
	}
	
	public boolean hasNotes(){
		//TODO return get from FragmentNoteList
		return false;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.slider_butEditField) {
			listener.SliderEditField();
		} else if (v.getId() == R.id.slider_butAddNote) {
			//TODO send to FragmentNoteList
		} else if (v.getId() == R.id.slider_butShowElevation) {
			
		} else if (v.getId() == R.id.slider_butShowSoilType) {
			
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float eventY = event.getRawY();
		
		switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
            	listener.SliderDragDown((int)eventY);
               break; 
            }
            case MotionEvent.ACTION_UP:
            {     
            	listener.SliderDragUp((int)(eventY));
                 break;
            }
            case MotionEvent.ACTION_MOVE:
            {
            	listener.SliderDragDragging((int)(eventY));
                break;
            }
        }
        return true;
	}
	
	public boolean isAddingNote(){
		return this.addingNote;
	}
	
	private OnMapClickListener sliderMapClickListener = new OnMapClickListener(){
		@Override
		public void onMapClick(LatLng arg0) {
			currentPoint = new MyMarker(map, arg0);
			if(currentPoint != null){
				currentNote.addMyMarker(currentPoint); //Adds a myPoint
			}
			map.setOnMapClickListener((OnMapClickListener) listener);
			if(fragmentDrawing != null) fragmentDrawing.setPointIcon(R.drawable.add_point_v1);
			addingPoint = false;
		}
	};

	@Override
	public void DrawingClickPoint() {
		if(addingPoint == false){
			map.setOnMapClickListener(sliderMapClickListener);
			fragmentDrawing.setPointIcon(R.drawable.cancel_point_v1);
			addingPoint = true;
		} else {
			map.setOnMapClickListener((OnMapClickListener) listener);
			fragmentDrawing.setPointIcon(R.drawable.add_point_v1);
			addingPoint = false;
		}		
	}

	@Override
	public void DrawingClickPolyline() {
		if(addingPolyline == false){
			currentPolyline = new MyPolyline(map);
			currentPolyline.edit();
			fragmentDrawing.setPolylineIcon(R.drawable.close_line_v1);
			addingPolyline = true;
		} else {
			if(currentPolyline != null) currentPolyline.complete();
			map.setOnMapClickListener((OnMapClickListener) listener);
			map.setOnMarkerClickListener((OnMarkerClickListener) listener);
			map.setOnMarkerDragListener((OnMarkerDragListener) listener);
			
			if(currentNote != null){
				//TODO handle edit finish? Maybe not, i think i removed on edit?
				currentPolyline.setColor(Field.STROKE_COLOR);
				currentNote.addMyPolyline(currentPolyline); //Adds a myPolyline
			}
			fragmentDrawing.setPolylineIcon(R.drawable.add_line_v1);
			addingPolyline = false;
		}
	}

	@Override
	public void DrawingClickPolygon() {
		if(addingPolygon == false){
			fragmentDrawing.setPolygonIcon(R.drawable.close_polygon);
			listener.SliderAddPolygon();
			addingPolygon = true;
		} else {
			fragmentDrawing.setPolygonIcon(R.drawable.add_polygon);
			listener.SliderCompletePolygon();
			addingPolygon = false;
		}		
	}

	@Override
	public void DrawingClickColor() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Pick a color");
		CharSequence colors[] = {"Red", "Yellow", "Blue", "Green"};
		builder.setItems(colors, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {
            	   int intColor = Color.GREEN;
            	   if(which == 0){
            		   //Red
            		   intColor = Color.RED;
            	   } else if(which == 1){
            		   //Yellow
            		   intColor = Color.YELLOW;
            	   } else if(which == 2){
            		   //Blue
            		   intColor = Color.BLUE;
            	   } else {
            		   //Green
            		   intColor = Color.GREEN;
            	   }
            	   //redraw polygons/polylines/points with new color
            	   currentNote.setColor(intColor);
               }
		});
		AlertDialog dialog = builder.create();
		dialog.show();		
	}

	@Override
	public void DrawingClickCamera() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Geotag Photo?");
		builder.setMessage("Would you like to associate this picture with a point?")
           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   
               }
           })
           .setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   
               }
           });
		AlertDialog dialog = builder.create();
		dialog.show();		
	}

}