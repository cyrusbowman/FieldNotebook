package com.openatk.fieldnotebook.notelist;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.ScrollAutoView;
import com.openatk.fieldnotebook.FragmentDrawing.DrawingListener;
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Image;
import com.openatk.fieldnotebook.db.Note;
import com.openatk.fieldnotebook.db.TableFields;
import com.openatk.fieldnotebook.db.TableImages;
import com.openatk.fieldnotebook.db.TableNotes;
import com.openatk.fieldnotebook.drawing.MyMarker;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolyline;
import com.openatk.fieldnotebook.imageviewer.FragmentImageViewer;
import com.openatk.fieldnotebook.imageviewer.ImageViewerListener;

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
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

public class FragmentNoteList extends Fragment implements OnClickListener, DrawingListener, ImageViewerListener {
	private static String TAG = FragmentNoteList.class.getName();

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
	private Bitmap imageBitmap;
	private Bitmap bitmap;
	private File image;

	LayoutInflater vi;

	private Boolean addingPolygon = false;
	private Boolean addingPolyline = false;
	private Boolean addingPoint = false;

	private Boolean addingNote = false;  //Or editing note

	private MyPolyline currentPolyline = null;
	private MyMarker currentPoint = null;

	private String imagePath = null;
	private Image currentImage = null; //Current image for imageviewer

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
			currentField = Field.FindFieldById(dbHelper.getReadableDatabase(), currentFieldId);
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
			Log.w("currentNote", Boolean.toString(currentNote == null));
			if (currentNote !=null) {
				View newView = inflateOpenNote(currentNote);
				currentOpenNoteView = (OpenNoteView) newView.getTag();

				//Show drawing fragment
				fragmentDrawing = listener.NoteListShowDrawing();
				fragmentDrawing.setListener(me);
			}
		} else {
			notes = null;
		}
	}

	public void finishPolygon(MyPolygon newPolygon){
		if(currentNote != null){
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
		//Bitmap bitmap = Bitmap.createBitmap(bounds.width() + 5, bounds.height(), conf);


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
		//Add Images as Markers with icons
		List<Image> images = note.getImages();
		for (int i = 0; i < images.size(); i++) {
			try {
				ExifInterface exifInt = new ExifInterface(images.get(i).getPath());
				imageBitmap = BitmapFactory.decodeByteArray(exifInt.getThumbnail(),0,exifInt.getThumbnail().length);
				imageBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth()/2,imageBitmap.getHeight()/2, false);
				float[] ltlng = new float[2];
				exifInt.getLatLong(ltlng);
				LatLng picLoc = new LatLng(ltlng[0],ltlng[1]);
				Log.w("bitmap null", Boolean.toString(imageBitmap == null));
				Log.w("bitmap descriptor", Boolean.toString(BitmapDescriptorFactory.fromBitmap(imageBitmap) == null));
//				map.addMarker(new MarkerOptions().position(picLoc).icon(BitmapDescriptorFactory.fromBitmap(imageBitmap)));
				note.addImageMarker(map.addMarker(new MarkerOptions().position(picLoc).icon(BitmapDescriptorFactory.fromBitmap(imageBitmap))));
//				note.addMyMarker(new MyMarker(marker, map));
			} catch (IOException e) {
				e.printStackTrace();
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
			svNotes.setScrollingEnabled(false);
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
		noteView.layFullNote = (RelativeLayout) view.findViewById(R.id.note_open_note);
		noteView.layImageView = (FrameLayout) view.findViewById(R.id.note_open_imageViewer);
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

		//Add images
		List<Image> images = note.getImages();		
		if(images != null){
			Log.d("FragmentNoteList - inflateOpenNote", "Images not null : " + Integer.toString(images.size()));
			for(int i=0; i<images.size(); i++){
				ImageView img = new ImageView(this.getActivity());
				Drawable d = new BitmapDrawable(this.getResources(), images.get(i).getThumb());
				img.setBackgroundDrawable(d);
				img.setTag(images.get(i));
				img.setOnClickListener(imageClickListener);

				float scale = getResources().getDisplayMetrics().density;
				int dpAsPixels = (int) (7*scale + 0.5f); //Margin
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				layoutParams.setMargins(dpAsPixels, 0, dpAsPixels, 0);
				noteView.layObjects.addView(img, layoutParams);
			}
		} else {
			Log.d("FragmentNoteList - inflateOpenNote", "Images null");
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
				svNotes.setScrollingEnabled(true);

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
						currentPolyline.setColor(Field.STROKE_COLOR);
						currentNote.addMyPolyline(currentPolyline); // Adds a
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
		RelativeLayout layFullNote;
		FrameLayout layImageView;
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
		note.myPolygonsToStringPolygons();
		//Save the polygons
		values.put(TableNotes.COL_POLYGONS, note.getStrPolygons());
		Log.d("SaveNote", "StrPolygons:" + note.getStrPolygons());
		//Save current my polylines to strpolylines
		note.myPolylinesToStringPolylines();
		//Save the polylines
		values.put(TableNotes.COL_LINES, note.getStrPolylines());
		Log.d("SaveNote", "StrPolylines:" + note.getStrPolylines());
		//Save current my mymarkers to strmarkers
		note.myMarkersToStringMarkers();
		//Save the markers
		values.put(TableNotes.COL_POINTS, note.getStrMarkers());
		Log.d("SaveNote", "StrPoints:" + note.getStrMarkers());
		//Save the imagemarkers
		note.imageMarkersToStringImageMarkers();
		values.put(TableNotes.COL_IMAGEPOINTS, note.getStrImageMarkers());
		Log.d("SaveNote", "StrImagePoints:" + note.getStrImageMarkers());

		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		List<Note> allNotes = Note.FindNotesByFieldName(dbHelper.getReadableDatabase(), note.getFieldName());
		for (int z = 0; z < allNotes.size(); z++) {
			Note aNote = allNotes.get(z);
			if(aNote.getId() != note.getId()) {
				// Get all the polygons, add their vertices to the bounding box
				List<PolygonOptions> listPolygons = aNote.getPolygons();
				Log.w("ListPolygons is empty:", Boolean.toString(listPolygons.isEmpty()) + " " + note.getComment());
				for (int i = 0; i < listPolygons.size(); i++) {
					List<LatLng> points = listPolygons.get(i).getPoints();
					for (int j = 0; j < points.size(); j++) {
						builder.include(points.get(j));
					}
				}
	
				// Do the same thing with the polylines
				List<PolylineOptions> listPolyline = aNote.getPolylines();
				for (int p = 0; p < listPolyline.size(); p++) {
					List<LatLng> points = listPolyline.get(p).getPoints();
					for (int u = 0; u < points.size(); u++) {
						builder.include(points.get(u));
					}
				}
	
				// Do the same thing with the markers
				List<MarkerOptions> listMyMarker= aNote.getMarkers();
				for (int p = 0; p < listMyMarker.size(); p++) {
					builder.include(listMyMarker.get(p).getPosition());
				}
				
				List<MarkerOptions> listImageMarker = aNote.getImageMarkers();
				Log.w("imagemarkers A", listImageMarker.toString());
				for (int p = 0; p < listImageMarker.size(); p++) {
					builder.include(listImageMarker.get(p).getPosition());
				}
			}
		}
		
		// Get all the polygons, add their vertices to the bounding box
		List<MyPolygon> listPolygons = note.getMyPolygons();
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

		// Do the same thing with the markers
		List<MyMarker> listMyMarker= note.getMyMarkers();
		for (int p = 0; p < listMyMarker.size(); p++) {
			builder.include(listMyMarker.get(p).getPosition());
		}
		
		List<MarkerOptions> listImageMarker = note.getImageMarkers();
		Log.w("imagemarkers B", listImageMarker.toString());
		for (int p = 0; p < listImageMarker.size(); p++) {
			builder.include(listImageMarker.get(p).getPosition());
		}

		// Create the boundary, get the borders, convert to a string
		try {
			LatLngBounds bounds = builder.build();
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
			database.update(TableFields.TABLE_NAME, valuesField, whereField, null);

			// Now draw this field
			// Create polygon

			List<LatLng> points = Field.StringToBoundary(newFieldBoundary);

			if(points != null && points.isEmpty() == false) {
				PolygonOptions polygonOptions = new PolygonOptions();
				polygonOptions.fillColor(Field.FILL_COLOR_NOT_PLANNED);
				polygonOptions.strokeWidth(Field.STROKE_WIDTH);
				polygonOptions.strokeColor(Field.STROKE_COLOR);
				for (int i = 0; i < points.size(); i++) {
					polygonOptions.add(points.get(i));
				}
				if (currentField == null) {
					Log.w("currentfield", "sdf");
				} else if (currentField.getPolygon() == null) {
					Log.w("currentfield get poly", "ok");
				}
				this.listener.NoteListUpdatePolygon(currentField.getId(), polygonOptions);
			}
		}
		catch (IllegalStateException e){
		}
		
		//TODO more stuff
		if(note.getId() == null){
			//New note
			Integer newId = (int) database.insert(TableNotes.TABLE_NAME, null, values);
			note.setId(newId);
		} else {
			// Editing note
			String where = TableNotes.COL_ID + " = " + note.getId();
			database.update(TableNotes.TABLE_NAME, values, where, null);
		}

		List<Image> images = note.getImages();
		if(images != null){
			for(int i=0; i<images.size(); i++){
				ContentValues values2 = new ContentValues();
				Image image = images.get(i);
				if(image.getId() == null){
					//New image, need to save in database
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					image.getThumb().compress(Bitmap.CompressFormat.PNG, 100, stream);
					byte[] byteArray = stream.toByteArray();
					values2.put(TableImages.COL_IMAGE, byteArray);
					values2.put(TableImages.COL_NOTE_ID, note.getId());
					values2.put(TableImages.COL_PATH, image.getPath());
					Integer newId = (int) database.insert(TableImages.TABLE_NAME, null, values2);
					image.setId(newId);
				}
			}
		} else {
			Log.d("FragmentNoteList", "Images null");
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
				notes.get(i).removeImageMarkers();
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

	//	@Override
	//	public void onClick(View v) {
	//
	//		if (v.getId() == R.id.slider_butShowElevation) {
	//			
	//
	//		} else if (v.getId() == R.id.slider_butShowSoilType) {
	//
	//		}
	//	}


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
			// path to /data/data/yourapp/app_data/imageDir
			String file = UUID.randomUUID().toString() + ".jpg"; //Random filename
	// Create imageDir
	File f = new File(this.getActivity().getFilesDir(), file);
	imagePath = f.getAbsolutePath();
	//Create new file
	FileOutputStream fos;
	try {
		fos = this.getActivity().openFileOutput(file, Context.MODE_WORLD_WRITEABLE);
		fos.close();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	//Get reference to the file
	File newf = new File(this.getActivity().getFilesDir(), file);

	Uri outputFileUri = Uri.fromFile(newf);
	Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); 
	cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
	this.getActivity().startActivityForResult(cameraIntent, 999);
}
public void ImageCaptured(){
	if(imagePath != null){
		Log.d(TAG, "ImageCaptured");
		Bitmap fullImage = BitmapFactory.decodeFile(imagePath);
		Bitmap thumb = ThumbnailUtils.extractThumbnail(fullImage, 100, 100);
		ImageView img = new ImageView(this.getActivity());
		img.setOnClickListener(imageClickListener);
		img.setTag(new Image(thumb, imagePath));

		float scale = getResources().getDisplayMetrics().density;
		int dpAsPixels = (int) (7*scale + 0.5f); //Margin
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(dpAsPixels, 0, dpAsPixels, 0);

		Drawable d = new BitmapDrawable(getResources(), thumb);
		img.setBackgroundDrawable(d);			
		this.currentOpenNoteView.layObjects.addView(img, layoutParams);

		//Save path and thumbnail in database
		if(currentNote != null){
			currentNote.addImage(thumb, imagePath);
		}
		try {
			ExifInterface exifInt = new ExifInterface(imagePath);
			imageBitmap = Bitmap.createScaledBitmap(thumb, thumb.getWidth()/2,thumb.getHeight()/2, false);
			float[] ltlng = new float[2];
			exifInt.getLatLong(ltlng);
			LatLng picLoc = new LatLng(ltlng[0],ltlng[1]);
			currentNote.addImageMarker(map.addMarker(new MarkerOptions().position(picLoc).icon(BitmapDescriptorFactory.fromBitmap(imageBitmap))));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
private OnClickListener imageClickListener = new OnClickListener(){
	@Override
	public void onClick(View v) {
		currentImage = (Image) v.getTag(); 
		int height = currentOpenNoteView.layFullNote.getHeight();
		currentOpenNoteView.layFullNote.setVisibility(View.GONE);
		currentOpenNoteView.layImageView.setVisibility(View.VISIBLE);
		//currentOpenNoteView.me.setBackgroundResource(R.drawable.note_image_viewer);
		// Gets the layout params that will allow you to resize the layout
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) currentOpenNoteView.layImageView.getLayoutParams();
		// Changes the height and width to the specified *pixels*
		params.height = height;
		currentOpenNoteView.layImageView.setLayoutParams(params);

		// Prepare a transaction to add fragments to this fragment
		FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
		// Add the list fragment to this fragment's layout
		Log.i(TAG, "onCreate: adding FragmentNoteList to FragmentSidebar");
		// Add the fragment to the this fragment's container layout
		FragmentImageViewer fragmentImageViewer = new FragmentImageViewer();
		fragmentTransaction.replace(currentOpenNoteView.layImageView.getId(), fragmentImageViewer, FragmentImageViewer.class.getName());
		// Commit the transaction
		fragmentTransaction.commit();
	}
};
@Override
public void ImageViewerRequestData(FragmentImageViewer requester) {
	if(this.currentNote != null){
		requester.populateData(this.currentNote.getImages(), currentImage);
	}
}
@Override
public void ImageViewerDone(Image image) {
	currentOpenNoteView.layFullNote.setVisibility(View.VISIBLE);
	currentOpenNoteView.layImageView.setVisibility(View.GONE);
	//currentOpenNoteView.me.setBackgroundResource(R.drawable.note);
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

@Override
public void onClick(View arg0) {	
}
}