package com.openatk.fieldnotebook.notelist;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openatk.fieldnotebook.FragmentDrawing;
import com.openatk.fieldnotebook.FragmentDrawing.DrawingListener;
import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.ScrollAutoView;
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Note;
import com.openatk.fieldnotebook.db.TableNotes;
import com.openatk.fieldnotebook.drawing.MyMarker;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolyline;

public class FragmentNoteList extends Fragment implements OnClickListener, DrawingListener {
	private static final int REQUEST_CODE = 1;

	private FragmentDrawing fragmentDrawing = null;

	private FragmentNoteList me = null;
	private GoogleMap map;

	private ScrollAutoView svNotes;
	private LinearLayout listNotes;
	
	private NoteListListener listener;
	private Field currentField = null;
	private List<Note> notes = null;

	private DatabaseHelper dbHelper;
	private Note currentNote = null;
	OpenNoteView currentOpenNoteView = null;
	private RelativeLayout currentNoteView = null;
	private Bitmap bitmap;
	private File image;

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
		View view = inflater.inflate(R.layout.fragment_note_list, container,
				false);

		me = this;
		
		svNotes = (ScrollAutoView) view.findViewById(R.id.note_list_scrollView);
		listNotes = (LinearLayout) view.findViewById(R.id.note_list_listNotes);
		
		dbHelper = new DatabaseHelper(this.getActivity());
		vi = (LayoutInflater) this.getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Fragment parentFragment = getParentFragment();
		if (parentFragment != null && parentFragment instanceof NoteListListener) {
			// Check if parent fragment (if there is one) is listener
			listener = (NoteListListener) parentFragment;
		} else if (activity != null && activity instanceof NoteListListener) {
			// Otherwise, check if parent activity is listener
			listener = (NoteListListener) activity;
		} else if(parentFragment != null && parentFragment instanceof NoteListParentListener){
			//Otherwise check if parent fragment knows who the listener is
			listener = ((NoteListParentListener)parentFragment).NoteListGetListener();
		} else if(activity != null && activity instanceof NoteListParentListener){
			//Otherwise check if parent activity knows who the listener is
			listener = ((NoteListParentListener)activity).NoteListGetListener();
		}
		else if (listener == null) {
			Log.w("FragmentNoteList", "onAttach: neither the parent fragment or parent activity implement NoteListListener");
			throw new ClassCastException("Parent Activity or parent fragment must implement NoteListListener");
		}
		Log.d("FragmentNoteList", "Attached");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener.NoteListRequestData(this);
	}

	public void populateData(Integer currentFieldId, GoogleMap map) {
		Log.d("FragmentNoteList", "PopulateData");
		this.map = map;

		// Clear current
		listNotes.removeAllViews();
		this.onClose();

		// Get current field
		currentField = null;
		if (currentFieldId != null) {
			currentField = Field.FindFieldById(dbHelper.getReadableDatabase(),
					currentFieldId);
			dbHelper.close();
		}
		if (currentField != null) {

			//Add all notes for this field
			notes = Note.FindNotesByFieldName(dbHelper.getReadableDatabase(), currentField.getName());
			dbHelper.close();
			for (int i = 0; i < notes.size(); i++) {
				// Add note to list
				listNotes.addView(inflateNote(notes.get(i)));
			}
		} else {
			notes = null;
		}
	}
		
	public void finishPolygon(MyPolygon newPolygon){
		if(currentNote != null){
			//TODO handle edit finish? Maybe not, i think i removed on edit?
			newPolygon.setStrokeColor(Field.STROKE_COLOR);
			currentNote.addMyPolygon(newPolygon); // Adds a mypolygon
		}
	}

	private View inflateNote(Note note) {
		View view = vi.inflate(R.layout.note, null);
		NoteView noteView = new NoteView();
		noteView.layNote = (RelativeLayout) view.findViewById(R.id.note);
		noteView.imgColor = (ImageView) view.findViewById(R.id.note_imgColor);
		noteView.butShowHide = (ImageButton) view.findViewById(R.id.note_butShowHide);
		noteView.tvComment = (TextView) view.findViewById(R.id.note_txtComment);
		noteView.imgPoints = (ImageView) view.findViewById(R.id.note_imgPoints);
		noteView.imgLines = (ImageView) view.findViewById(R.id.note_imgLines);
		noteView.imgPolygons = (ImageView) view
				.findViewById(R.id.note_imgPolygons);
		noteView.row1 = (RelativeLayout) view.findViewById(R.id.note_row1);
		noteView.row2 = (RelativeLayout) view.findViewById(R.id.note_row2);

		noteView.note = note;

		noteView.tvComment.setText(note.getComment());

		
		noteView.tvComment.setTag(noteView);

		noteView.butShowHide.setTag(noteView);
		noteView.layNote.setTag(noteView);
		
		noteView.tvComment.setOnClickListener(noteClickListener);
		noteView.butShowHide.setOnClickListener(noteClickListener);
		noteView.layNote.setOnClickListener(noteClickListener);
		
		if(note.getVisible() == 1){
			noteView.butShowHide.setImageResource(R.drawable.note_but_hide);
		} else {
			noteView.butShowHide.setImageResource(R.drawable.note_but_show);
		}
		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		paint.setShadowLayer(2f, 0f, 2f, Color.LTGRAY);
		paint.setTextAlign(Align.RIGHT);
		paint.setTextSize(20);
		paint.setStrokeWidth(20);
		
		
		
		//Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		//Bitmap bitmap = Bitmap.createBitmap(bounds.width() + 5, bounds.height(), conf); //TODO create blank new bitmap

		
		//Add polygons from note to map
		List<MyPolygon> myPolygons = note.getMyPolygons();
		if(myPolygons.isEmpty()){
			List<PolygonOptions> polygons = note.getPolygons(); //Gets map polygons
			for(int i=0; i<polygons.size(); i++){
				Polygon newPolygon = map.addPolygon(polygons.get(i));
				note.addMyPolygon(new MyPolygon(map, newPolygon)); //Adds back my polygons
			}
		} else {
			for (int i = 0; i < myPolygons.size(); i++) {
				myPolygons.get(i).unselect();
			}
		}
		// Add polylines from note to map
		List<MyPolyline> myPolylines = note.getMyPolylines();
		if (myPolylines.isEmpty()) {
			List<PolylineOptions> polylines = note.getPolylines(); // Gets map
																	// polygons
			for (int i = 0; i < polylines.size(); i++) {
				note.addMyPolyline(new MyPolyline(map.addPolyline(polylines
						.get(i)), map)); // Adds back my polygons
			}
		} else {
			for (int i = 0; i < myPolylines.size(); i++) {
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
		

		//Show icon and draw number on icon
		Integer numberOfPolygons = note.getMyPolygons().size();
		if(numberOfPolygons == 0){
			noteView.imgPolygons.setVisibility(View.GONE);
		} else {
			noteView.imgPolygons.setVisibility(View.VISIBLE);
			String label = Integer.toString(numberOfPolygons);
			Bitmap bitmap = decodeMutableBitmapFromResourceId(this.getActivity(), R.drawable.polygon);
			Rect bounds = new Rect();
			paint.getTextBounds(label, 0, label.length(), bounds);
			float x = bitmap.getWidth() - 2.0f;
			float y = -1.0f * bounds.top + (bitmap.getHeight() * 0.06f);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawText(label, x, y, paint);
			BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
			noteView.imgPolygons.setBackgroundDrawable(ob);
		}
		Integer numberOfPolylines = note.getMyPolylines().size();
		if(numberOfPolylines == 0){
			noteView.imgLines.setVisibility(View.GONE);
		} else {
			noteView.imgLines.setVisibility(View.VISIBLE);
			String label = Integer.toString(numberOfPolylines);
			Bitmap bitmap = decodeMutableBitmapFromResourceId(this.getActivity(), R.drawable.line_v1);
			Rect bounds = new Rect();
			paint.getTextBounds(label, 0, label.length(), bounds);
			float x = bitmap.getWidth() - 2.0f;
			float y = -1.0f * bounds.top + (bitmap.getHeight() * 0.06f);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawText(label, x, y, paint);
			BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
			noteView.imgLines.setBackgroundDrawable(ob);
		}
		Integer numberOfPoints = note.getMyMarkers().size();
		if(numberOfPoints == 0){
			noteView.imgPoints.setVisibility(View.GONE);
		} else {
			noteView.imgPoints.setVisibility(View.VISIBLE);
			String label = Integer.toString(numberOfPoints);
			Bitmap bitmap = decodeMutableBitmapFromResourceId(this.getActivity(), R.drawable.point);
			Rect bounds = new Rect();
			paint.getTextBounds(label, 0, label.length(), bounds);
			float x = bitmap.getWidth() - 2.0f;
			float y = -1.0f * bounds.top + (bitmap.getHeight() * 0.06f);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawText(label, x, y, paint);
			BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
			noteView.imgPoints.setBackgroundDrawable(ob);
		}
		

		noteView.me = view;
		view.setTag(noteView);
		return view;
	}

	// a random comment

	private OnClickListener noteClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			NoteView noteView = (NoteView) v.getTag();
			if(v.getId() == R.id.note_butShowHide){
				
			} else if(v.getId() == R.id.note_txtComment){ 
				if(addingNote == false){
					addingNote = true;
					//Set focus
					OpenNoteView newView = openNote(noteView);
					newView.etComment.requestFocus();
					//Show keyboard
					InputMethodManager inputMethodManager = (InputMethodManager) me.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				    if (inputMethodManager != null) {
				        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
				    }
				}
			} else if(v.getId() == R.id.note){
				if(addingNote == false){
					addingNote = true;

					openNote(noteView);

				}

			}

		}
		
		private OpenNoteView openNote(NoteView noteView){
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
			return currentOpenNoteView;
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
		noteView.layObjects = (LinearLayout) view.findViewById(R.id.note_open_lay_objects);
		noteView.svObjects = (HorizontalScrollView) view.findViewById(R.id.note_open_sv_objects);

		noteView.etComment.setText(note.getComment());
		
		List<MyPolygon> polygons = note.getMyPolygons();
		List<MyPolyline> polylines = note.getMyPolylines();
		List<MyMarker> markers = note.getMyMarkers();
		
		for(int i=0; i<polygons.size(); i++){
			ImageView img = new ImageView(this.getActivity());
			img.setBackgroundResource(R.drawable.polygon);
			noteView.layObjects.addView(img);
		}
		for(int i=0; i<polylines.size(); i++){
			ImageView img = new ImageView(this.getActivity());
			img.setBackgroundResource(R.drawable.line_v1);
			noteView.layObjects.addView(img);
		}
		for(int i=0; i<markers.size(); i++){
			ImageView img = new ImageView(this.getActivity());
			img.setBackgroundResource(R.drawable.point);
			noteView.layObjects.addView(img);
		}
		
		noteView.note = note;

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

	private File createImageFile() throws IOException {
		// Create an image file name
		File storageDir = new File(Environment
				.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES).toString());
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = "IMG_" + timeStamp + "_";
		image = File.createTempFile(imageFileName, ".jpg", storageDir);
		Log.w("createimagefile", Boolean.toString(image == null));
		// mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(image.getAbsolutePath());
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.getActivity().getApplicationContext().sendBroadcast(mediaScanIntent);
	}

	private OnClickListener openNoteClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			OpenNoteView noteView = (OpenNoteView) v.getTag();
			if(v.getId() == R.id.note_open_butDone){
				if(addingPolygon){
					fragmentDrawing.setPolygonIcon(R.drawable.add_polygon);
					listener.NoteListCompletePolygon();
					addingPolygon = false;
				}

				if (addingPolyline) {
					if (currentPolyline != null)
						currentPolyline.complete();
					map.setOnMapClickListener((OnMapClickListener) listener);
					map.setOnMarkerClickListener((OnMarkerClickListener) listener);
					map.setOnMarkerDragListener((OnMarkerDragListener) listener);

					if (currentNote != null) {
						// TODO handle edit finish? Maybe not, i think i removed
						// on edit?
						currentPolyline.setColor(Field.STROKE_COLOR);
						currentNote.addMyPolyline(currentPolyline); // Adds a
																	// myPolyline
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
				// Close the note
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
		HorizontalScrollView svObjects;
		LinearLayout layObjects;
		Note note;
		View me;
		ImageButton openCamera;
	}

	private void SaveNote(Note note) {
		addingNote = false;

		SQLiteDatabase database = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(TableNotes.COL_COMMENT,note.getComment());
		values.put(TableNotes.COL_FIELD_NAME,note.getFieldName());
		values.put(TableNotes.COL_COLOR,note.getColor());

		//Save current my polygons to strpolygons

		// Iaman and Patrick added this for demo 2

		// Get all the polygons, add their vertices to the bounding box
		List<MyPolygon> listPolygons = note.getMyPolygons();
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		for (int i = 0; i < listPolygons.size(); i++) {
			List<LatLng> points = listPolygons.get(i).getPoints();
			for (int j = 0; j < points.size(); j++) {
				builder.include(points.get(j));
			}
		}

		// Do the same thing with the polylines
		List<MyPolyline> listPolyline = note.getMyPolylines();
		for (int p = 0; p < listPolyline.size(); p++) {
			List<LatLng> points = listPolyline.get(p).getPoints();
			for (int u = 0; u < points.size(); u++) {
				builder.include(points.get(u));
			}
		}

		// TODO: there aren't markers or images yet, but add in the same test
		// for those
		// once they exist.

		// Create the boundary, get the borders, convert to a string
		/*LatLngBounds bounds = builder.build();
		LatLng northeast = bounds.northeast;
		LatLng southwest = bounds.southwest;
		String newFieldBoundary = Double.toString(southwest.latitude) + ","
				+ Double.toString(southwest.longitude) + ","
				+ Double.toString(southwest.latitude) + ","
				+ Double.toString(northeast.longitude) + ","
				+ Double.toString(northeast.latitude) + ","
				+ Double.toString(northeast.longitude) + ","
				+ Double.toString(northeast.latitude) + ","
				+ Double.toString(southwest.longitude);// and so on

		// Update the database with the new boundary value:
		ContentValues valuesField = new ContentValues();
		valuesField.put(TableFields.COL_BOUNDARY, newFieldBoundary);
		String whereField = TableFields.COL_NAME + "= '" + note.getFieldName()
				+ "'";
		database.update(TableFields.TABLE_NAME, valuesField, whereField, null);*/
		
		
		// map.clear();
		// MainActivity.drawFields();

		// Done with case file boundary creation

		// TODO more stuff
		if (note.getId() == null) {
			// New note
			database.insert(TableNotes.TABLE_NAME, null, values);
		} else {
			// Editing note
			String where = TableNotes.COL_ID + " = " + note.getId();
			database.update(TableNotes.TABLE_NAME, values, where, null);
		}
		database.close();
		dbHelper.close();
	}

	public void onMapClick(LatLng position) {
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

	public void onClose() {
		// Remove all notes polygons
		Log.d("FragmentSlider", "onClose");
		if (notes != null) {
			for (int i = 0; i < notes.size(); i++) {
				notes.get(i).removePolygons();
				notes.get(i).removePolylines();
				notes.get(i).removeMarkers();
			}
		}
	}
	

	public int oneNoteHeight() {
		if (currentNoteView != null) {
			RelativeLayout layout = (RelativeLayout) currentNoteView
					.findViewById(R.id.note_open);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			Log.d("Height:", Integer.toString(params.height));
			return params.height;
		}
		return 0;
	}

	public boolean hasNotes() {
		if (notes != null && notes.size() > 0) {
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
		Log.w("drawclickcam imageest", Boolean.toString(image == null));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Geotag Photo?");
		builder.setMessage("Would you like to associate this picture with a point?")
           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
            	// Capture image from camera - added 10/31
   				int CAMERA_PIC_REQUEST = 1337;
   				Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

   				File f = null;
   				try {		
   					f = createImageFile();
					Log.w("after createimage ", Boolean.toString(image == null));
   					Log.w("click_listener", image.toString());
   				} catch (IOException e) {
   					// TODO Auto-generated catch block
   					e.printStackTrace();
   				}
   				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
				Log.w("after putextra", Boolean.toString(image == null));
   				startActivityForResult(intent, CAMERA_PIC_REQUEST);
   				
				Log.w("after startact", Boolean.toString(image == null));

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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.w("onact imagetest", Boolean.toString(image == null));
		Log.w("onActivityResult", "1");
		Log.w("RESULT_OK",Integer.toString(resultCode));
		if (resultCode == Activity.RESULT_OK) {
			Log.w("REQUEST_OK",Integer.toString(requestCode));
			if (requestCode == REQUEST_CODE) {
				// We need to recycle unused bitmaps
				if (bitmap != null) {
					bitmap.recycle();
				}
//				Bundle extras = data.getExtras();
//				bitmap = (Bitmap) extras.get("data");
				
				
				//InputStream stream = getActivity().getContentResolver().openInputStream(data.getData());
				Log.w("InputStream","2");
				//bitmap = BitmapFactory.decodeStream(stream);
				//stream.close();
//				galleryAddPic();
				try {
					Log.w("exif", Boolean.toString(image == null));
					ExifInterface xint = new ExifInterface(image.getAbsolutePath().toString());
					Log.w("image",image.getAbsolutePath().toString());
					
					bitmap = BitmapFactory.decodeByteArray(xint.getThumbnail(),0,xint.getThumbnail().length);
					bitmap=Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/2,bitmap.getHeight()/2, false);
					float[] ltlng = new float[2];
					xint.getLatLong(ltlng);
//					LatLng picLoc = new LatLng(Double.parseDouble(xint.getAttribute(xint.TAG_GPS_LATITUDE)),Double.parseDouble(xint.getAttribute(xint.TAG_GPS_LONGITUDE)));
					LatLng picLoc = new LatLng(ltlng[0],ltlng[1]);
					//Log.w("picLoc",picLoc.toString());
					//Log.w("bitmap", bitmap.toString());
					Marker imageMarker = map.addMarker(new MarkerOptions().position(picLoc).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	

	public Boolean AddNote() {
		if(addingNote == false){
			Log.d("FragmentNoteList", "AddNote");
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
			return true;
		}		
		return false;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static Bitmap decodeMutableBitmapFromResourceId(final Context context, final int bitmapResId) {
	    final Options bitmapOptions = new Options();
	    if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)
	        bitmapOptions.inMutable = true;
	    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), bitmapResId, bitmapOptions);
	    if (!bitmap.isMutable())
	        bitmap = convertToMutable(context, bitmap);
	    return bitmap;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static Bitmap convertToMutable(final Context context, final Bitmap imgIn) {
	    final int width = imgIn.getWidth(), height = imgIn.getHeight();
	    final Config type = imgIn.getConfig();
	    File outputFile = null;
	    final File outputDir = context.getCacheDir();
	    try {
	        outputFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null, outputDir);
	        outputFile.deleteOnExit();
	        final RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
	        final FileChannel channel = randomAccessFile.getChannel();
	        final MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
	        imgIn.copyPixelsToBuffer(map);
	        imgIn.recycle();
	        final Bitmap result = Bitmap.createBitmap(width, height, type);
	        map.position(0);
	        result.copyPixelsFromBuffer(map);
	        channel.close();
	        randomAccessFile.close();
	        outputFile.delete();
	        return result;
	    } catch (final Exception e) {
	    } finally {
	        if (outputFile != null)
	            outputFile.delete();
	    }
	    return null;
	}

}