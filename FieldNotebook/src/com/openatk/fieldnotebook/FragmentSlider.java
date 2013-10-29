package com.openatk.fieldnotebook;

import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Note;
import com.openatk.fieldnotebook.db.TableFields;
import com.openatk.fieldnotebook.db.TableNotes;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolyline;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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

	
	private GoogleMap map;
	private TextView tvName;
	private TextView tvAcres;
	private ImageButton butEditField;
	
	private Button butShowElevation;
	private Button butShowSoilType;
	private Button butAddNote;
	private ScrollAutoView svNotes;
	private LinearLayout listNotes;
	
	private SliderListener listener;
	private Field currentField = null;
	private List<Note> notes = null;
	
	private DatabaseHelper dbHelper;
	private Note currentNote = null;
	OpenNoteView currentOpenNoteView = null;
	private RelativeLayout currentNoteView = null;
	
	LayoutInflater vi;
	
	private Boolean addingPolygon = false;
	private Boolean addingPolyline = false;
	private Boolean addingNote = false;  //Or editing note
	
	private MyPolyline currentPolyline = null;
		
	// Interface for receiving data
	public interface SliderListener {
		public void SliderDragDown(int start);
		public void SliderDragDragging(int whereY);
		public void SliderDragUp(int whereY);
		public void SliderEditField();
		public void SliderRequestData();
		public void  SliderCompletePolygon();
		public void SliderAddPolygon();
		public void SliderEditPolygon(MyPolygon poly);
		public void SliderAddNote();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_slider, container,
				false);

		tvName = (TextView) view.findViewById(R.id.slider_tvName);
		tvAcres = (TextView) view.findViewById(R.id.slider_tvAcres);
		
		view.setOnTouchListener(this);
		tvName.setOnTouchListener(this);
		tvAcres.setOnTouchListener(this);
		
		butEditField = (ImageButton) view.findViewById(R.id.slider_butEditField);
		butShowElevation = (Button) view.findViewById(R.id.slider_butShowElevation);
		butShowSoilType = (Button) view.findViewById(R.id.slider_butShowSoilType);
		butAddNote = (Button) view.findViewById(R.id.slider_butAddNote);

		svNotes = (ScrollAutoView) view.findViewById(R.id.slider_scrollView);
		listNotes = (LinearLayout) view.findViewById(R.id.slider_listNotes);
		
		svNotes.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER); //Visual bottom of scroll effect

		
		butEditField.setOnClickListener(this);
		butShowElevation.setOnClickListener(this);
		butShowSoilType.setOnClickListener(this);
		butAddNote.setOnClickListener(this);
		
		dbHelper = new DatabaseHelper(this.getActivity());
		vi = (LayoutInflater) this.getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener.SliderRequestData();
	}

	public void populateData(Integer currentFieldId, GoogleMap map) {
		this.map = map;
				
		//Clear current
		listNotes.removeAllViews();
		this.onClose();
		
		//Get current field
		currentField = null;
		if(currentFieldId != null){
			currentField = Field.FindFieldById(dbHelper.getReadableDatabase(), currentFieldId);
			dbHelper.close();
		}
		if (currentField != null) {
			tvName.setText(currentField.getName());
			tvAcres.setText(Integer.toString(currentField.getAcres()) + " ac");
			//Add all notes for this field
			notes = Note.FindNotesByFieldName(dbHelper.getReadableDatabase(), currentField.getName());
			dbHelper.close();
			for(int i=0; i<notes.size(); i++){
				//Add note to list
				listNotes.addView(inflateNote(notes.get(i)));
			}
		} else {
			tvName.setText("");
			tvAcres.setText("");
			notes = null;
		}
	}
	
	public void finishPolygon(MyPolygon newPolygon){
		if(currentNote != null){
			//TODO handle edit finish? Maybe not, i think i removed on edit?
			newPolygon.setStrokeColor(Field.STROKE_COLOR);
			currentNote.addMyPolygon(newPolygon); //Adds a mypolygon
		}
	}
	
	private View inflateNote(Note note){
		View view = vi.inflate(R.layout.note, null);
		NoteView noteView = new NoteView();
		noteView.layNote = (RelativeLayout) view.findViewById(R.id.note);
		noteView.butEdit = (ImageButton) view.findViewById(R.id.note_butEdit);
		noteView.butShowHide = (ImageButton) view.findViewById(R.id.note_butShowHide);
		noteView.tvComment = (TextView) view.findViewById(R.id.note_txtComment);
		noteView.tvComment2 = (TextView) view.findViewById(R.id.note_txtComment2);
		noteView.imgPoints = (ImageView) view.findViewById(R.id.note_imgPoints);
		noteView.imgLines = (ImageView) view.findViewById(R.id.note_imgLines);
		noteView.imgPolygons = (ImageView) view.findViewById(R.id.note_imgPolygons);
		noteView.row1 = (RelativeLayout) view.findViewById(R.id.note_row1);
		noteView.row2 = (RelativeLayout) view.findViewById(R.id.note_row2);
		noteView.note = note;
		
		noteView.tvComment.setText(note.getComment());
		
		noteView.butEdit.setTag(noteView);
		noteView.butShowHide.setTag(noteView);
		noteView.row1.setTag(noteView);

		noteView.butEdit.setOnClickListener(noteClickListener);
		noteView.butShowHide.setOnClickListener(noteClickListener);
		noteView.row1.setOnClickListener(noteClickListener);
		
		if(note.getVisible() == 1){
			noteView.butShowHide.setImageResource(R.drawable.note_but_hide);
		} else {
			noteView.butShowHide.setImageResource(R.drawable.note_but_show);
		}
		
		
		//Add polygons from note to map
		List<MyPolygon> myPolygons = note.getMyPolygons();
		if(myPolygons.isEmpty()){
			List<PolygonOptions> polygons = note.getPolygons(); //Gets map polygons
			for(int i=0; i<polygons.size(); i++){
				note.addMyPolygon(new MyPolygon(map, map.addPolygon(polygons.get(i)))); //Adds back my polygons
			}
		} else {
			for(int i =0; i<myPolygons.size(); i++){
				myPolygons.get(i).unselect();
			}
		}
		//Add polylines from note to map
		List<MyPolyline> myPolylines = note.getMyPolylines();
		if(myPolylines.isEmpty()){
			List<PolylineOptions> polylines = note.getPolylines(); //Gets map polygons
			for(int i=0; i<polylines.size(); i++){
				note.addMyPolyline(new MyPolyline(map.addPolyline(polylines.get(i)), map)); //Adds back my polygons
			}
		} else {
			for(int i =0; i<myPolylines.size(); i++){
				myPolylines.get(i).unselect();
			}
		}
		
		noteView.me = view;
		view.setTag(noteView);
		return view;
	}
	
	private OnClickListener noteClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			NoteView noteView = (NoteView) v.getTag();
			if(v.getId() == R.id.note_butShowHide){
				
			} else if(v.getId() == R.id.note_row1){
				if(addingNote == false){
					addingNote = true;
					
					
					svNotes.scrollToAfterAdd(noteView.me.getTop());
					//Edit this note
					int index = listNotes.indexOfChild(noteView.me);
					currentNote = noteView.note;
					listNotes.removeView(noteView.me);
					View newView = inflateOpenNote(currentNote);
					listNotes.addView(newView, index);
					currentOpenNoteView = (OpenNoteView) newView.getTag();
					Log.d("Current Scroll:", Float.toString(svNotes.getScrollY()));
					Log.d("v Top:", Integer.toString(v.getTop()));
					Log.d("v Bottom:", Integer.toString(v.getBottom()));
					Log.d("me Top:", Integer.toString(newView.getTop()));
					Log.d("me Bottom:", Integer.toString(newView.getBottom()));
				}
			}
		}
	};
	
	static class NoteView
    {
		ImageButton butEdit;
		ImageButton butShowHide;
		TextView tvComment;
		TextView tvComment2;
		ImageView imgPoints;
		ImageView imgLines;
		ImageView imgPolygons;
		RelativeLayout row1;
		RelativeLayout row2;
		RelativeLayout layNote;
		Note note;
		View me;
    }
	
	
	private View inflateOpenNote(Note note){
		View view = vi.inflate(R.layout.note_open, null);
		final OpenNoteView noteView = new OpenNoteView();
		noteView.layNote = (RelativeLayout) view.findViewById(R.id.note_open);
		noteView.butPoint = (ImageButton) view.findViewById(R.id.note_open_butPoint);
		noteView.butLine = (ImageButton) view.findViewById(R.id.note_open_butLine);
		noteView.butPolygon = (ImageButton) view.findViewById(R.id.note_open_butPolygon);
		noteView.butColor = (ImageButton) view.findViewById(R.id.note_open_butColor);
		noteView.butDone = (ImageButton) view.findViewById(R.id.note_open_butDone);
		noteView.butDelete = (ImageButton) view.findViewById(R.id.note_open_butDelete);
		noteView.etComment = (EditText) view.findViewById(R.id.note_open_etComment);
		
		noteView.note = note;
		
		noteView.etComment.setText(note.getComment());
		noteView.etComment.setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER);
		noteView.etComment.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int keyCode, KeyEvent event) {
		        if (event != null && (event.getAction() == KeyEvent.ACTION_DOWN) &&  (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
		        {               
		           // hide virtual keyboard
		           InputMethodManager imm =  (InputMethodManager)getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		           imm.hideSoftInputFromWindow(noteView.etComment.getWindowToken(), 0);
		           return true;
		        }
		        return false;
		    }
		});
		
		noteView.butPoint.setTag(noteView);
		noteView.butLine.setTag(noteView);
		noteView.butPolygon.setTag(noteView);
		noteView.butColor.setTag(noteView);
		noteView.butDone.setTag(noteView);
		noteView.butDelete.setTag(noteView);
		
		noteView.butPoint.setOnClickListener(openNoteClickListener);
		noteView.butLine.setOnClickListener(openNoteClickListener);
		noteView.butPolygon.setOnClickListener(openNoteClickListener);
		noteView.butColor.setOnClickListener(openNoteClickListener);
		noteView.butDone.setOnClickListener(openNoteClickListener);
		noteView.butDelete.setOnClickListener(openNoteClickListener);
		noteView.me = view;
		
		view.setTag(noteView);
		return view;
	}
	private OnClickListener openNoteClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			OpenNoteView noteView = (OpenNoteView) v.getTag();
			currentNote = noteView.note;
			if(v.getId() == R.id.note_open_butPoint){
				
			} else if(v.getId() == R.id.note_open_butLine){
				if(addingPolyline == false){
					currentPolyline = new MyPolyline(map);
					currentPolyline.edit();
					noteView.butLine.setImageResource(R.drawable.close_line_v1);
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
					noteView.butLine.setImageResource(R.drawable.add_line_v1);
					addingPolyline = false;
				}
			} else if(v.getId() == R.id.note_open_butPolygon){
				if(addingPolygon == false){
					noteView.butPolygon.setImageResource(R.drawable.close_polygon);
					listener.SliderAddPolygon();
					addingPolygon = true;
				} else {
					noteView.butPolygon.setImageResource(R.drawable.add_polygon);
					listener.SliderCompletePolygon();
					addingPolygon = false;
				}
			} else if(v.getId() == R.id.note_open_butColor){
				
			} else if(v.getId() == R.id.note_open_butDone){
				if(addingPolygon){
					noteView.butPolygon.setImageResource(R.drawable.add_polygon);
					listener.SliderCompletePolygon();
					addingPolygon = false;
				}
				
				if(addingPolyline){
					if(currentPolyline != null) currentPolyline.complete();
					map.setOnMapClickListener((OnMapClickListener) listener);
					map.setOnMarkerClickListener((OnMarkerClickListener) listener);
					map.setOnMarkerDragListener((OnMarkerDragListener) listener);
					
					if(currentNote != null){
						//TODO handle edit finish? Maybe not, i think i removed on edit?
						currentPolyline.setColor(Field.STROKE_COLOR);
						currentNote.addMyPolyline(currentPolyline); //Adds a myPolyline
					}
					noteView.butLine.setImageResource(R.drawable.add_line_v1);
					addingPolygon = false;
				}
				
				//Save the note
				currentNote.setComment(noteView.etComment.getText().toString());
				SaveNote(currentNote);
				//Close the note
				int index = listNotes.indexOfChild(noteView.me);
				listNotes.removeView(noteView.me);
				listNotes.addView(inflateNote(currentNote), index);
			} else if(v.getId() == R.id.note_open_butDelete){
				
			}
		}
	};
	static class OpenNoteView
    {
		ImageButton butPoint;
		ImageButton butLine;
		ImageButton butPolygon;
		ImageButton butColor;
		ImageButton butDone;
		ImageButton butDelete;
		EditText etComment;
		RelativeLayout layNote;
		Note note;
		View me;
    }
	
	private void SaveNote(Note note){
		addingNote = false;
		
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(TableNotes.COL_COMMENT,note.getComment());
		values.put(TableNotes.COL_FIELD_NAME,note.getFieldName());
		
		//Save current my polygons to strpolygons
		note.myPolygonsToStringPolygons();
		//Save the polygons
		values.put(TableNotes.COL_POLYGONS, note.getStrPolygons());
		Log.d("SaveNote", "StrPolygons:" + note.getStrPolygons());
		//Save current my polylines to strpolylines
		note.myPolylinesToStringPolylines();
		//Save the polylines
		values.put(TableNotes.COL_LINES, note.getStrPolylines());
		Log.d("SaveNote", "StrPolylines:" + note.getStrPolylines());
		
		// Iaman and Patrick added this for demo 2
	
		// Get all the polygons, add their vertices to the bounding box
		List<MyPolygon> listPolygons = note.getMyPolygons();
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		for(int i = 0;i<listPolygons.size();i++){
			List<LatLng> points = listPolygons.get(i).getPoints();
			for(int j=0;j<points.size();j++){
				builder.include(points.get(j));
			}
		}
		
		// Do the same thing with the polylines
		List<MyPolyline> listPolyline = note.getMyPolylines();
		for(int p=0;p<listPolyline.size();p++){
			List<LatLng> points = listPolyline.get(p).getPoints();
			for(int u = 0;u<points.size();u++){
				builder.include(points.get(u));
			}
		}
		
		// TODO: there aren't markers or images yet, but add in the same test for those
		// once they exist.
		
		// Create the boundary, get the borders, convert to a string
		LatLngBounds bounds = builder.build();
		LatLng northeast = bounds.northeast;
		LatLng southwest = bounds.southwest;
		String newFieldBoundary = Double.toString(southwest.latitude) + "," + Double.toString(southwest.longitude) + ","+ Double.toString(southwest.latitude)+"," + Double.toString(northeast.longitude)+ "," + Double.toString(northeast.latitude)+","+ Double.toString(northeast.longitude)+ ","+Double.toString(northeast.latitude)+","+Double.toString(southwest.longitude);//and so on
		//newFieldBoundary should look like this: "Lat,Lng,Lat,Lng,Lat,Lng,Lat,Lng"
		
		// Update the database with the new boundary value:
		ContentValues valuesField = new ContentValues();
		valuesField.put(TableFields.COL_BOUNDARY, newFieldBoundary);
		String whereField = TableFields.COL_NAME + "= '" + note.getFieldName() + "'";
		database.update(TableFields.TABLE_NAME, valuesField, whereField, null);
		
		// Done with case file boundary creation
		
		//TODO more stuff
		if(note.getId() == null){
			//New note
			database.insert(TableNotes.TABLE_NAME, null, values);
		} else {
			//Editing note
			String where = TableNotes.COL_ID + " = " + note.getId();
			database.update(TableNotes.TABLE_NAME, values, where, null);
		}
		database.close();
		dbHelper.close();
	}
	
	public void onMapClick(LatLng position){
		Log.d("Here", "FragmentSlider - onMapClick");
		//Check if clicked on any of current notes objects
		if(this.currentNote != null){
			//Loop through current notes polygons checking if touched
			List<MyPolygon> polys = this.currentNote.getMyPolygons();
			MyPolygon touchedPoly = null;
			for(int i=0; i<polys.size(); i++){
				if(polys.get(i).wasTouched(position)){
					touchedPoly = polys.get(i);
					break;
				}
			}
			if(touchedPoly != null){
				touchedPoly.edit();
				if(this.currentOpenNoteView != null){
					this.currentOpenNoteView.butPolygon.setImageResource(R.drawable.close_polygon);
				}
				//Shouldn't recieve touch if already adding so this is fine
				this.currentNote.removePolygon(touchedPoly);
				listener.SliderEditPolygon(touchedPoly);
				addingPolygon = true;
			}
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
	
	public void onClose(){
		//Remove all notes polygons
		Log.d("FragmentSlider", "onClose");
		if(notes != null){
			for(int i=0; i<notes.size(); i++){
				notes.get(i).removePolygons();
				notes.get(i).removePolylines();
			}
		}
	}

	public int getHeight() {
		// Method so close transition can work
		return getView().getHeight();
	}
	
	public int oneNoteHeight() {
		if(currentNoteView != null){
			RelativeLayout layout = (RelativeLayout) currentNoteView.findViewById(R.id.note_open);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			Log.d("Height:", Integer.toString(params.height));
			return params.height;
		}
		return 0;
	}
	
	public boolean hasNotes(){
		if(notes != null && notes.size() > 0){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.slider_butEditField) {
			listener.SliderEditField();
		} else if (v.getId() == R.id.slider_butAddNote) {
			if(addingNote == false){
				this.addingNote = true;
				//Add a new note
				Note newNote = new Note(currentField.getName());
				notes.add(newNote);
				
				View newView = inflateOpenNote(newNote);
				currentOpenNoteView = (OpenNoteView) newView.getTag();
				listNotes.addView(newView, 0);
				listener.SliderAddNote();
				
				svNotes.scrollTo(0, 0);
				//InputMethodManager inputMethodManager = (InputMethodManager) this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				//inputMethodManager.showSoftInput(newOpenNote.etComment, 0);
			}
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

}