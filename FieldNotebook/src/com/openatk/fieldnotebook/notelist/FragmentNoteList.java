package com.openatk.fieldnotebook.notelist;

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
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Note;
import com.openatk.fieldnotebook.db.TableNotes;
import com.openatk.fieldnotebook.drawing.MyMarker;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolyline;
import com.openatk.fieldnotebook.slider.SliderListener;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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

public class FragmentNoteList extends Fragment implements OnClickListener, DrawingListener, SliderListener {
	private FragmentDrawing fragmentDrawing = null;
	private FragmentNoteList me = null;
	private GoogleMap map;
	private TextView tvName;
	private TextView tvAcres;
	private ImageButton butEditField;
	
	private Button butShowElevation;
	private Button butShowSoilType;
	private Button butAddNote;
	private ScrollAutoView svNotes;
	private LinearLayout listNotes;
	
	private NoteListListener listener;
	private Field currentField = null;
	private List<Note> notes = null;
	
	private DatabaseHelper dbHelper;
	private Note currentNote = null;
	OpenNoteView currentOpenNoteView = null;
	private RelativeLayout currentNoteView = null;
	
	LayoutInflater vi;
	
	private Boolean addingPolygon = false;
	private Boolean addingPolyline = false;
	private Boolean addingPoint = false;

	private Boolean addingNote = false;  //Or editing note
	
	private MyPolyline currentPolyline = null;
	private MyMarker currentPoint = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_slider, container,
				false);

		me = this;
				
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
		listener.NoteListRequestData(this);
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
		noteView.imgColor = (ImageView) view.findViewById(R.id.note_imgColor);
		noteView.butShowHide = (ImageButton) view.findViewById(R.id.note_butShowHide);
		noteView.tvComment = (TextView) view.findViewById(R.id.note_txtComment);
		noteView.imgPoints = (ImageView) view.findViewById(R.id.note_imgPoints);
		noteView.imgLines = (ImageView) view.findViewById(R.id.note_imgLines);
		noteView.imgPolygons = (ImageView) view.findViewById(R.id.note_imgPolygons);
		noteView.row1 = (RelativeLayout) view.findViewById(R.id.note_row1);
		noteView.row2 = (RelativeLayout) view.findViewById(R.id.note_row2);
		noteView.note = note;
		
		noteView.tvComment.setText(note.getComment());
		
		noteView.butShowHide.setTag(noteView);
		noteView.layNote.setTag(noteView);

		noteView.butShowHide.setOnClickListener(noteClickListener);
		noteView.layNote.setOnClickListener(noteClickListener);
		
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
				Polygon newPolygon = map.addPolygon(polygons.get(i));
				note.addMyPolygon(new MyPolygon(map, newPolygon)); //Adds back my polygons
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
		//Add points from note to map
		List<MyMarker> myMarkers = note.getMyMarkers();
		if(myMarkers.isEmpty()){
			List<MarkerOptions> markers = note.getMarkers(); //Gets map markers
			for(int i=0; i<markers.size(); i++){
				note.addMyMarker(new MyMarker(map.addMarker(markers.get(i)), map)); //Adds back my markers
			}
		} else {
			for(int i =0; i<myMarkers.size(); i++){
				myMarkers.get(i).unselect();
			}
		}
		note.setColor(note.getColor());
		noteView.imgColor.setBackgroundColor(note.getColor());
		
		
		noteView.me = view;
		view.setTag(noteView);
		return view;
	}
	
	private OnClickListener noteClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			NoteView noteView = (NoteView) v.getTag();
			if(v.getId() == R.id.note_butShowHide){
				
			} else if(v.getId() == R.id.note){
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
					
					//Show drawing fragment
					fragmentDrawing = listener.NoteListShowDrawing();
					fragmentDrawing.setListener(me);
				}
			}
		}
	};
	
	static class NoteView
    {
		ImageView imgColor;
		ImageButton butShowHide;
		TextView tvComment;
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
		noteView.butDone = (ImageButton) view.findViewById(R.id.note_open_butDone);
		noteView.butDelete = (ImageButton) view.findViewById(R.id.note_open_butDelete);
		noteView.etComment = (EditText) view.findViewById(R.id.note_open_etComment);
		
		noteView.note = note;
		noteView.etComment.setText(note.getComment());
		
		/*noteView.etComment.setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER);
		noteView.etComment.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int keyCode, KeyEvent event) {
		    	Log.d("EtEvent", "EtEvent");
		        if (event != null && (event.getAction() == KeyEvent.ACTION_DOWN) &&  (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
		        {               
		           // hide virtual keyboard
		           InputMethodManager imm =  (InputMethodManager)getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		           imm.hideSoftInputFromWindow(noteView.etComment.getWindowToken(), 0);
		           return true;
		        }
		        return false;
		    }
		});*/
		
	
		noteView.butDone.setTag(noteView);
		noteView.butDelete.setTag(noteView);
		
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
			if(v.getId() == R.id.note_open_butDone){
				if(addingPolygon){
					fragmentDrawing.setPolygonIcon(R.drawable.add_polygon);
					listener.NoteListCompletePolygon();
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
					fragmentDrawing.setPolylineIcon(R.drawable.add_line_v1);
					addingPolygon = false;
				}
				
				// hide virtual keyboard
		        InputMethodManager imm =  (InputMethodManager)getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		        imm.hideSoftInputFromWindow(noteView.etComment.getWindowToken(), 0);
		        
				//Save the note
				currentNote.setComment(noteView.etComment.getText().toString());
				SaveNote(currentNote);
				//Close the note
				int index = listNotes.indexOfChild(noteView.me);
				listNotes.removeView(noteView.me);
				listNotes.addView(inflateNote(currentNote), index);
				
				//Hide drawing fragment
				listener.NoteListHideDrawing();
				fragmentDrawing = null;
				
				
			} else if(v.getId() == R.id.note_open_butDelete){
				
			}
		}
	};
	static class OpenNoteView
    {
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
		values.put(TableNotes.COL_COLOR,note.getColor());

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
		//Save current my polylines to strpolylines
		note.myMarkersToStringMarkers();
		//Save the polylines
		values.put(TableNotes.COL_POINTS, note.getStrMarkers());
		Log.d("SaveNote", "StrPoints:" + note.getStrMarkers());
		
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
			//Check if touched polyline
			List<MyPolyline> polylines = this.currentNote.getMyPolylines();
			MyPolyline touchedPolyline = null;
			for(int i=0; i<polylines.size(); i++){
				Log.d("Checking Polyline touch...", "Checking...");
				if(polylines.get(i).wasTouched(position)){
					Log.d("Checking Polyline touch == ", "TRUE");
					touchedPolyline = polylines.get(i);
					break;
				}
			}
			if(touchedPolyline != null){
				//Touched a polyline, edit it
				touchedPolyline.edit();
				if(this.currentOpenNoteView != null){
					if(fragmentDrawing != null) fragmentDrawing.setPolylineIcon(R.drawable.close_line_v1);
				}
				currentPolyline = touchedPolyline;
				this.currentNote.removePolyline(touchedPolyline);
				addingPolyline = true;
			} else {
				//Check if touched polygon
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
						if(fragmentDrawing != null) fragmentDrawing.setPolygonIcon(R.drawable.close_polygon);
					}
					//Shouldn't recieve touch if already adding so this is fine
					this.currentNote.removePolygon(touchedPoly);
					listener.NoteListEditPolygon(touchedPoly);
					addingPolygon = true;
				}
			}
		}
	}
	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		
		// Check if parent fragment (if there is one) implements the image
		// selection interface
		Fragment parentFragment = getParentFragment();
		if (parentFragment != null && parentFragment instanceof NoteListListener) {
			listener = (NoteListListener) parentFragment;
		}
		// Otherwise, check if parent activity implements the image
		// selection interface
		else if (activity != null && activity instanceof NoteListListener) {
			listener = (NoteListListener) activity;
		}
		else if (listener == null) {
			Log.w("FragmentNoteList", "onAttach: neither the parent fragment or parent activity implement NoteListListener");
			throw new ClassCastException("Parent Activity or parent fragment must implement NoteListListener");
		}
	
		Log.d("FragmentNoteList", "Attached");
	}
	
	public void onClose(){
		//Remove all notes polygons
		Log.d("FragmentSlider", "onClose");
		if(notes != null){
			for(int i=0; i<notes.size(); i++){
				notes.get(i).removePolygons();
				notes.get(i).removePolylines();
				notes.get(i).removeMarkers();
			}
		}
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
		if (v.getId() == R.id.slider_butShowElevation) {
			
		} else if (v.getId() == R.id.slider_butShowSoilType) {
			
		}
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
			listener.NoteListAddPolygon();
			addingPolygon = true;
		} else {
			fragmentDrawing.setPolygonIcon(R.drawable.add_polygon);
			listener.NoteListCompletePolygon();
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

	@Override
	public void SliderAddNote() {
		if(addingNote == false){
			this.addingNote = true;
			//Add a new note
			Note newNote = new Note(currentField.getName());
			notes.add(newNote);
			currentNote = newNote;
			
			View newView = inflateOpenNote(newNote);
			currentOpenNoteView = (OpenNoteView) newView.getTag();
			listNotes.addView(newView, 0);
			listener.NoteListAddNote();
			
			svNotes.scrollTo(0, 0);
			//InputMethodManager inputMethodManager = (InputMethodManager) this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			//inputMethodManager.showSoftInput(newOpenNote.etComment, 0);
			
			//Show drawing fragment
			fragmentDrawing = listener.NoteListShowDrawing();
			fragmentDrawing.setListener(me);
		}		
	}

}