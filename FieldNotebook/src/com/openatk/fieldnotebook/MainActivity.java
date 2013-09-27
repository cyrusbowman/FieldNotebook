package com.openatk.fieldnotebook;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.openatk.fieldnotebook.FragmentAddField.AddFieldListener;
import com.openatk.fieldnotebook.FragmentSlider.SliderListener;
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.TableFields;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolygon.MyPolygonListener;

public class MainActivity extends FragmentActivity implements OnClickListener,
		OnMapClickListener, OnItemSelectedListener, OnMarkerClickListener, OnMarkerDragListener,
		AddFieldListener, SliderListener, MyPolygonListener {
	
	private GoogleMap map;
	private UiSettings mapSettings;
	
	
    //Startup position
 	private static final float START_LAT = 40.428712f;
 	private static final float START_LNG = -86.913819f;
 	private static final float START_ZOOM = 17.0f;

	private Menu menu;
	private DatabaseHelper dbHelper;
	
	private int mCurrentState = 0;
	private int sliderIsShowing = 0;
	private int addIsShowing = 0;


	private Field currentField = null;
	private MyPolygon currentPolygon = null;

	private List<Field> FieldsOnMap = null;

	String addingBoundary = "";
	
	FragmentAddField fragmentAddField = null;
	FragmentSlider fragmentSlider = null;
	
	
	private static final int STATE_DEFAULT = 0;
	private static final int STATE_LIST_VIEW = 1;
	
	//Trello
    //SyncController syncController;
    //TrelloController trelloController;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dbHelper = new DatabaseHelper(this);
		
		FragmentManager fm = getSupportFragmentManager();
		SupportMapFragment f = (SupportMapFragment) fm.findFragmentById(R.id.map);
		fragmentSlider = (FragmentSlider) fm.findFragmentByTag("slider");
		if(fragmentSlider != null){
			sliderIsShowing = 1;
		}
		
		if (savedInstanceState == null) {
			// First incarnation of this activity.
			f.setRetainInstance(true);
		} else {
			// Reincarnated activity. The obtained map is the same map instance
			// in the previous
			// activity life cycle. There is no need to reinitialize it.
			map = f.getMap();
		}
		checkGPS();

		//Trello
        //trelloController = new TrelloController(getApplicationContext());
        //syncController = new SyncController(getApplicationContext(), trelloController, this);
        //trelloController.setSyncController(syncController);

		// Get last selected operation
		if (savedInstanceState != null) {
			// Find current field
			currentField = FindFieldById(savedInstanceState.getInt("currentField"));
			this.addingBoundary = savedInstanceState.getString("drawingBoundary", "");
		}

		setUpMapIfNeeded();
		
		Intent intent = this.getIntent();
		String todo = intent.getStringExtra("todo");
		if(todo != null){
			if(todo.contentEquals("sync")){
				//trelloController.sync();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(addIsShowing == 1){
			//Save current polygon
			List<LatLng> points = this.currentPolygon.getPoints();
			Boolean wasAnEdit = false;
			if (currentField == null) {
				//Save to outState				
				points = this.currentPolygon.getMarkers();
			} else {
				currentField.setBoundary(points);
				wasAnEdit = true;
			}

			String strNewBoundary = "";
			if(points != null && points.isEmpty() == false){
				// Generate boundary
				StringBuilder newBoundary = new StringBuilder(
						points.size() * 20);
				for (int i = 0; i < points.size(); i++) {
					newBoundary.append(points.get(i).latitude);
					newBoundary.append(",");
					newBoundary.append(points.get(i).longitude);
					newBoundary.append(",");
				}
				newBoundary.deleteCharAt(newBoundary.length() - 1);
				strNewBoundary = newBoundary.toString();
			}
			if(wasAnEdit){
				// Save this field to the db
				SQLiteDatabase database = dbHelper.getWritableDatabase();
				ContentValues values = new ContentValues();
				values.put(TableFields.COL_BOUNDARY, strNewBoundary);
				database.update(TableFields.TABLE_NAME,values,TableFields.COL_ID + " = "+ Integer.toString(currentField.getId()),null);
				dbHelper.close();
			} else {
				outState.putString("drawingBoundary", strNewBoundary);
			}
		}
		
		if (currentField != null) outState.putInt("currentField", currentField.getId());
		
		outState.putInt("mCurrentState", mCurrentState);
		outState.putInt("sliderIsShowing",sliderIsShowing);
		outState.putInt("addIsShowing",addIsShowing);

		super.onSaveInstanceState(outState);
	}

	private void setUpMapIfNeeded() {
		if (map == null) {
			map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		}
		// markerHandler = new MarkerHandler(map, this, mCurrentRockSelected);
		// slideMenu.setMarkerHandler(markerHandler);
		if (map != null) {
			mapSettings = map.getUiSettings();
			mapSettings.setZoomControlsEnabled(false);
			mapSettings.setMyLocationButtonEnabled(false);
			mapSettings.setTiltGesturesEnabled(false);
			
			map.setOnMapClickListener(this);
			map.setOnMarkerClickListener(this);
			map.setOnMarkerDragListener(this);
			map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			map.setMyLocationEnabled(true);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    		Float startLat = prefs.getFloat("StartupLat", START_LAT);
    		Float startLng = prefs.getFloat("StartupLng", START_LNG);
    		Float startZoom = prefs.getFloat("StartupZoom", START_ZOOM);
    		map.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(startLat,startLng) , startZoom));
		}
		drawMap();
	}

	
	
	@Override
	protected void onPause() {
		super.onPause();
        //trelloController.stopAutoSync();
		
        CameraPosition myCam = map.getCameraPosition();
		if(myCam != null){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
			LatLng where = myCam.target;
			editor.putFloat("StartupLat", (float) where.latitude);
			editor.putFloat("StartupLng",(float) where.longitude); 
			editor.putFloat("StartupZoom",(float) map.getCameraPosition().zoom); 
			editor.commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkGPS();
        //trelloController.startAutoSync();   
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String todo = intent.getStringExtra("todo");
		if(todo != null){
			if(todo.contentEquals("sync")){
				//trelloController.sync();
			}
		}
	}

	
	@Override
	public void onMapClick(LatLng position) {
		if (addIsShowing == 1 || addingNotePolygon) {
			Log.d("HERE", "Here1");
			// Add points to polygon
			this.currentPolygon.addPoint(position);
		} else if(this.fragmentSlider != null && this.fragmentSlider.isAddingNote() == true) {
			Log.d("HERE", "Here2");

			//Adding a note, give the note the click events
			this.fragmentSlider.onMapClick(position);
		} else {
			Log.d("HERE", "Here3");
			if(this.fragmentSlider == null){
				Log.d("HERE", "Here null");
			}

			// Map view
			if(fragmentSlider != null){
				//fragmentSlider.flushChangesAndSave(false); //Save changes to all open notes
			}
			
			// Check if touched a field
			Boolean touched = false;
			for (int i = 0; i < FieldsOnMap.size(); i++) {
				Field curField = FieldsOnMap.get(i);
				if (curField.wasTouched(position)) {
					// Touched this field
					touched = true;

					if (this.currentPolygon != null) {
						// Set back to unselected if one is selected
						this.currentPolygon.unselect();
					}

					// Load field and job data and show edit menu
					Boolean sameField = false;
					Field newField = FindFieldById(curField.getId());
					if(currentField != null && currentField.getId() == curField.getId()){
						sameField = true;
					}
					currentField = newField;
					currentPolygon = curField.getPolygon();
					this.currentPolygon.select();
					if (currentField == null) {
						Log.d("MainActivity - onMapClick", "unable to find field by id");
					} else {
						if(sameField == false){
							this.SliderRequestData(); //Populate slider again
						}
					}
					showSlider(true);
					break;
				}
			}
			if (touched == false) {
				// Close menu, save edits
				Log.d("MainActivity - onMapClick", "Close");
				if (this.currentPolygon != null) {
					// Set back to unselected
					this.currentPolygon.unselect();
				}
				hideSlider(true);
				this.currentField = null;
			}
		}
	}

	private void drawMap() {
		map.clear();
		drawFields();
	}

	
	private void drawFields() {
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		String[] columns = { TableFields.COL_ID, TableFields.COL_BOUNDARY, TableFields.COL_NAME, TableFields.COL_DELETED };
		String where = TableFields.COL_DELETED + " = 0";
		Cursor cursor = database.query(TableFields.TABLE_NAME, columns, where, null, null, null, null);
		FieldsOnMap = new ArrayList<Field>();
		while (cursor.moveToNext()) {
			String boundary = cursor.getString(cursor.getColumnIndex(TableFields.COL_BOUNDARY));
			List<LatLng> points = Field.StringToBoundary(boundary);
			
			if(points.size() == 0) points = null;
			
			// Add to list so we can catch click events
			Field newField = new Field();
			newField.setId(cursor.getInt(cursor.getColumnIndex(TableFields.COL_ID)));
			newField.setMap(map);
			newField.setBoundary(points);
			newField.setName(cursor.getString(cursor.getColumnIndex(TableFields.COL_NAME)));

			// Now draw this field
			// Create polygon
			if(points != null && points.isEmpty() == false) {
				PolygonOptions polygonOptions = new PolygonOptions();
				polygonOptions.fillColor(Field.FILL_COLOR_NOT_PLANNED);
				polygonOptions.strokeWidth(Field.STROKE_WIDTH);
				polygonOptions.strokeColor(Field.STROKE_COLOR);
				for (int i = 0; i < points.size(); i++) {
					polygonOptions.add(points.get(i));
				}
				newField.setPolygon(new MyPolygon(map, map.addPolygon(polygonOptions), this));
				if (currentField != null && newField.getId() == currentField.getId()) {
					this.currentPolygon = newField.getPolygon();
					this.currentPolygon.setLabel(newField.getName(), true);
				} else {
					newField.getPolygon().setLabel(newField.getName());
				}
			}
			FieldsOnMap.add(newField);
		}
		cursor.close();
		dbHelper.close();
		if(addIsShowing == 1){
			if(this.currentPolygon != null && currentField != null){
				this.currentPolygon.edit();
			}
			if(this.addingBoundary.length() > 0){
				List<LatLng> points = Field.StringToBoundary(this.addingBoundary);
				this.currentPolygon = new MyPolygon(map, this);
				for(int i=0; i<(points.size()-1); i++){
					this.currentPolygon.addPoint(points.get(i));
				}
			}
		}
	}

	@Override
	public void onClick(View v) {

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if(addIsShowing == 1) {
			getMenuInflater().inflate(R.menu.add_field, menu);
		} else {
			getMenuInflater().inflate(R.menu.main, menu);
		}
		this.menu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.main_menu_add) {
			addFieldMapView();
		} else if (item.getItemId() == R.id.main_menu_current_location) {
			Location myLoc = map.getMyLocation();
			if(myLoc == null){
				Toast.makeText(this, "Still searching for your location", Toast.LENGTH_SHORT).show();
			} else {
				CameraPosition oldPos = map.getCameraPosition();
				CameraPosition newPos = new CameraPosition(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()), map.getMaxZoomLevel(), oldPos.tilt, oldPos.bearing);
				map.animateCamera(CameraUpdateFactory.newCameraPosition(newPos));
			}
		} else if (item.getItemId() == R.id.main_menu_list_view) {
			if(sliderIsShowing == 0){
				showSlider(true);
			} else {
				hideSlider(true);
			}
			if (mCurrentState == STATE_LIST_VIEW) {
				// Show map view
				Log.d("MainActivity", "Showing map view");
				setState(STATE_DEFAULT);
				//item.setIcon(R.drawable.list_view);
			} else {
				// Show list view
				Log.d("MainActivity", "Showing list view");
				setState(STATE_LIST_VIEW);
				//item.setIcon(R.drawable.map_view);
			}
		} else if(item.getItemId() == R.id.main_menu_help){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
	        alert.setTitle("Help");
	        WebView wv = new WebView(this);
	        wv.loadUrl("file:///android_asset/Help.html");
	        wv.getSettings().setSupportZoom(true);
	        wv.getSettings().setBuiltInZoomControls(true);
	        wv.setWebViewClient(new WebViewClient()
	        {
	            @Override
	            public boolean shouldOverrideUrlLoading(WebView view, String url)
	            {
	                view.loadUrl(url);
	                return true;
	            }
	        });
	        alert.setView(wv);
	        alert.setNegativeButton("Close", null);
	        alert.show();
		} else if(item.getItemId() == R.id.main_menu_legal){
			CharSequence licence= "The MIT License (MIT)\n" +
	                "\n" +
	                "Copyright (c) 2013 Purdue University\n" +
	                "\n" +
	                "Permission is hereby granted, free of charge, to any person obtaining a copy " +
	                "of this software and associated documentation files (the \"Software\"), to deal " +
	                "in the Software without restriction, including without limitation the rights " +
	                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
	                "copies of the Software, and to permit persons to whom the Software is " +
	                "furnished to do so, subject to the following conditions:" +
	                "\n" +
	                "The above copyright notice and this permission notice shall be included in " +
	                "all copies or substantial portions of the Software.\n" +
	                "\n" +
	                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
	                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
	                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
	                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
	                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
	                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN " +
	                "THE SOFTWARE.\n";
			new AlertDialog.Builder(this)
				.setTitle("Legal")
				.setMessage(licence)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton("Close", null).show();
		}
		return true;
	}

	private Void addFieldMapView() {
		// Add field (Polygon)
		currentField = null;
		showAdd(true);
		this.currentPolygon = new MyPolygon(map, this);
		return null;
	}

	private Void setState(int newState) {
		setState(newState, true);
		return null;
	}

	private void setState(int newState, boolean transition) {
		Log.d("SetState!!", "Setting state:" + Integer.toString(newState));
		if (mCurrentState == newState) {
			return;
		}
		// Exit current state
		if (mCurrentState == STATE_DEFAULT) {

		} else if (mCurrentState == STATE_LIST_VIEW) {
			/*FragmentManager fm = getSupportFragmentManager();
			// Hide list
			Fragment fragment = fm.findFragmentById(R.id.list_view);
			FragmentTransaction ft = fm.beginTransaction();
			ft.hide(fragment);
			ft.commit();
			fragmentListView = null;*/
		}

		// Enter new state
		if (newState == STATE_DEFAULT) {

		} else if (newState == STATE_LIST_VIEW) {
			/*FragmentManager fm = getSupportFragmentManager();
			// Show List
			FragmentListView fragmentListView = (FragmentListView) fm
					.findFragmentById(R.id.list_view);
			fragmentListView.getData();
			FragmentTransaction ft = fm.beginTransaction();
			ft.show(fragmentListView);
			ft.commit();

			this.fragmentListView = fragmentListView;*/
		}
		// Officially in new state
		mCurrentState = newState;
		this.invalidateOptionsMenu();
	}

	/*private Void showEdit(Boolean transition) {
		if (editIsShowing == 0) {
			hideAdd(false);
			editIsShowing = 1;
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_edit_job);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			layout.setLayoutParams(params);

			FragmentManager fm = getSupportFragmentManager();
			FragmentEditJobPopup fragment = new FragmentEditJobPopup();
			FragmentTransaction ft = fm.beginTransaction();
			if (transition)
				ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
			ft.add(R.id.fragment_container_edit_job, fragment, "edit_job");
			ft.commit();

			fragmentEditField = fragment;
		}
		return null;
	}

	private void hideEdit(Boolean transition) {
		if (editIsShowing == 1) {
			editIsShowing = 0;
			FragmentManager fm = getSupportFragmentManager();
			FragmentEditJobPopup fragment = (FragmentEditJobPopup) fm
					.findFragmentByTag("edit_job");
			// Set height so transition works
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_edit_job);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = fragment.getHeight();
			layout.setLayoutParams(params);
			// Do transition
			FragmentTransaction ft = fm.beginTransaction();
			if (transition)
				ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
			ft.remove(fragment);
			ft.commit();
			fragmentEditField = null;
		}
	}*/

	private Void showAdd(Boolean transition) {
		if (addIsShowing == 0) {
			addIsShowing = 1;
			hideSlider(false);
			// Set height back to wrap, in case add buttons or something
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_add_field);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			layout.setLayoutParams(params);

			FragmentManager fm = getSupportFragmentManager();
			FragmentAddField fragment = new FragmentAddField();
			FragmentTransaction ft = fm.beginTransaction();
			if (transition)
				ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
			ft.add(R.id.fragment_container_add_field, fragment, "add_field");
			ft.commit();
			fragmentAddField = fragment;
		}
		this.invalidateOptionsMenu();
		return null;
	}

	private void hideAdd(Boolean transition) {
		if (addIsShowing == 1) {
			addIsShowing = 0;
			FragmentManager fm = getSupportFragmentManager();
			FragmentAddField fragment = (FragmentAddField) fm
					.findFragmentByTag("add_field");
			// Set height so transition works
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_add_field);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = fragment.getHeight();
			layout.setLayoutParams(params);
			// Do transition
			FragmentTransaction ft = fm.beginTransaction();
			if (transition)
				ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
			ft.remove(fragment);
			ft.commit();
			fragmentAddField = null;
		}
		this.invalidateOptionsMenu();
	}	

	
	private Void showSlider(Boolean transition) {
		if(addIsShowing == 1){
			hideAdd(false);
		}
		if (sliderIsShowing == 0) {
			sliderIsShowing = 1;
			// Set height back to wrap, in case add buttons or something
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_slider);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			layout.setLayoutParams(params);
			FragmentManager fm = getSupportFragmentManager();
			FragmentSlider fragment = new FragmentSlider();
			FragmentTransaction ft = fm.beginTransaction();
			if (transition)
				ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
			ft.add(R.id.fragment_container_slider, fragment, "slider");
			ft.commit();
			fragmentSlider = fragment;
			sliderPosition = 0;
		}
		this.invalidateOptionsMenu();
		return null;
	}

	private void hideSlider(Boolean transition) {
		if (sliderIsShowing == 1) {
			sliderIsShowing = 0;
			if(fragmentSlider != null) fragmentSlider.onClose();
			
			FragmentManager fm = getSupportFragmentManager();
			FragmentSlider fragment = (FragmentSlider) fm.findFragmentByTag("slider");
			// Set height so transition works TODO 3 different heights?? Get from fragment, fragment.getMyHeight?
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_slider);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = fragment.getHeight();
			layout.setLayoutParams(params);
			// Do transition
			FragmentTransaction ft = fm.beginTransaction();
			if (transition)
				ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
			ft.remove(fragment);
			ft.commit();
			fragmentSlider = null;
		}
		this.invalidateOptionsMenu();
	}	
	
	private int sliderStartDrag = 0;
	private int sliderHeightStart = 0;
	@Override
	public void SliderDragDown(int start) {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int height = size.y;
		
		if(fragmentSlider != null){
			ScrollView sv = (ScrollView) fragmentSlider.getView().findViewById(R.id.slider_scrollView);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv.getLayoutParams();
			sliderStartDrag = height - start - params.height;
			sliderHeightStart = params.height;
		}
	}

	@Override
	public void SliderDragDragging(int whereY) {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int height = size.y;
		
		if(fragmentSlider != null){
			ScrollView sv = (ScrollView) fragmentSlider.getView().findViewById(R.id.slider_scrollView);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv.getLayoutParams();
			
			if((height - whereY - sliderStartDrag) > 0){
				params.height = height - whereY - sliderStartDrag;
			} else {
				params.height = 0;
			}
			sv.setLayoutParams(params);
		}
	}
	
	@Override
	public void SliderDragUp(int whereY) {
		//Slider done dragging snap to 1 of 3 positions
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int oneThirdHeight = size.y / 3;
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
		if(fragmentSlider != null){
			ScrollView sv = (ScrollView) fragmentSlider.getView().findViewById(R.id.slider_scrollView);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv.getLayoutParams();
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
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int oneThirdHeight = size.y / 3;
		
		if(fragmentSlider != null){
			ScrollView sv = (ScrollView) fragmentSlider.getView().findViewById(R.id.slider_scrollView);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv.getLayoutParams();
			if(sliderPosition == 2 || sliderPosition == 1){
				//Middle -> Small
				//OneNote -> Small
				DropDownAnim an = new DropDownAnim(sv, params.height, 0);
				an.setDuration(300);
				sv.startAnimation(an);
				sliderPosition = 0;
			} else if(sliderPosition == 3){
				//Fullscreen -> Middle if has notes
				//Fullscreen -> Small if no notes
				if(fragmentSlider.hasNotes()){
					DropDownAnim an = new DropDownAnim(sv, params.height, oneThirdHeight);
					an.setDuration(300);
					sv.startAnimation(an);
					sliderPosition = 2;
				} else {
					DropDownAnim an = new DropDownAnim(sv, params.height, 0);
					an.setDuration(300);
					sv.startAnimation(an);
					sliderPosition = 0;
				}
			}
			sv.setLayoutParams(params);
		}
	}
	private void SliderGrow(){
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int oneThirdHeight = size.y / 3;
		int actionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
		{
		    actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		}
		if(fragmentSlider != null){
			RelativeLayout relAdd = (RelativeLayout) fragmentSlider.getView().findViewById(R.id.slider_layMenu);
			Log.d("layMenu:", Integer.toString(relAdd.getHeight()));
			ScrollView sv = (ScrollView) fragmentSlider.getView().findViewById(R.id.slider_scrollView);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv.getLayoutParams();
			if(sliderPosition == 0 || sliderPosition == 1){
				//Small -> Middle
				//OneNote -> Middle
				DropDownAnim an = new DropDownAnim(sv, params.height, oneThirdHeight);
				an.setDuration(300);
				sv.startAnimation(an);
				sliderPosition = 2;
			} else if(sliderPosition == 2){
				//Middle -> Fullscreen
				DropDownAnim an = new DropDownAnim(sv, params.height, (size.y - relAdd.getHeight() - actionBarHeight));
				an.setDuration(300);
				sv.startAnimation(an);
				sliderPosition = 3;
			}
			sv.setLayoutParams(params);
		}
	}
	private void SliderOneNote(){
		if(fragmentSlider != null){
			RelativeLayout relAdd = (RelativeLayout) fragmentSlider.getView().findViewById(R.id.slider_layMenu);
			Log.d("layMenu:", Integer.toString(relAdd.getHeight()));
			ScrollView sv = (ScrollView) fragmentSlider.getView().findViewById(R.id.slider_scrollView);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv.getLayoutParams();
			
			DropDownAnim an = new DropDownAnim(sv, params.height, fragmentSlider.oneNoteHeight());
			an.setDuration(300);
			sv.startAnimation(an);
			sliderPosition = 1;
			
			sv.setLayoutParams(params);
		}
	}
	private MyPolygon saveFieldPolygon = null;
	private Boolean addingNotePolygon = false;
	@Override
	public void SliderAddPolygon() {
		// Add note polygon
		addingNotePolygon = true;
		saveFieldPolygon = this.currentPolygon;
		this.currentPolygon = new MyPolygon(map, this);
	}
	
	@Override
	public void SliderEditPolygon(MyPolygon poly) {
		// Add note polygon
		addingNotePolygon = true;
		saveFieldPolygon = this.currentPolygon;
		this.currentPolygon = poly;
	}
	
	@Override
	public void SliderCompletePolygon(){
		addingNotePolygon = false;
		this.currentPolygon.complete();
		//this.currentPolygon.setLabel(name, true);
		this.currentPolygon.setFillColor(Field.FILL_COLOR_PLANNED);
		if(this.fragmentSlider != null){
			this.fragmentSlider.finishPolygon(this.currentPolygon);
		}
		this.currentPolygon = saveFieldPolygon;
		saveFieldPolygon = null;
	}
	
	@Override
	public void SliderAddNote() {
		// Add Resize to oneNote Size
		this.SliderOneNote();
	}
	
	@Override
	public void SliderEditField() {
		showAdd(true);
		
		// Edit this fields points
		if(this.currentPolygon == null){
			this.currentPolygon = new MyPolygon(map, this);
		} else {
			this.currentPolygon.edit();
		}
	}

	@Override
	public void SliderRequestData() {
		if(this.fragmentSlider != null){
			Integer id = null;
			if(this.currentField != null) id = this.currentField.getId();
			this.fragmentSlider.populateData(id, map);
		}
	}
	
	public class DropDownAnim extends Animation {
	    int targetHeight;
	    int startHeight;
	    View view;
	    boolean down;

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
	
	/*
	@Override
	public void SyncControllerUpdateField(Integer localId) {
		//Check if field still exists, if so redraw boundary
		Field localField = this.FindFieldById(localId);
		if(localField != null) {
			MyPolygon polygon = null;
			for (int i = 0; i < FieldsOnMap.size(); i++) {
				if (FieldsOnMap.get(i).getId() == localField.getId()) {
					polygon = FieldsOnMap.get(i).getPolygon();
				}
			}
			
			if(polygon != null){
				//Redraw polygon
				polygon.updatePoints(localField.getBoundary());
			}
		}		
	}
	
	@Override
	public void SyncControllerDeleteField(Integer localId){
		//Check if field still exists, if so redraw boundary
		Field localField = this.FindFieldById(localId);
		if(localField != null) {
			SQLiteDatabase database = dbHelper.getWritableDatabase();
			database.delete(TableFields.TABLE_NAME, TableFields.COL_ID + " = " + Integer.toString(localId), null);
			dbHelper.close();
			
			MyPolygon polygon = null;
			for(int i=0; i<FieldsOnMap.size(); i++){
				if(FieldsOnMap.get(i).getId() == localField.getId()){
					polygon = FieldsOnMap.get(i).getPolygon();
					FieldsOnMap.remove(i);
				}
			}
			if(polygon != null){
				//Remove polygon
				polygon.remove();
			}
			
			if(this.currentField != null && this.currentField.getId() == localField.getId()){
				if(editIsShowing == 1) hideEdit(true);
				if(addIsShowing == 1) hideAdd(true);
				this.currentField = null;
				//Remove polygon
				if(this.currentPolygon != null){
					this.currentPolygon.delete();
					this.currentPolygon = null;
				}
			}
			if(this.fragmentListView != null) this.fragmentListView.getData();
		}
	}
	
	@Override
	public void SyncControllerAddField(Integer localId){
		//Check if field still exists, if so redraw boundary
		Field localField = this.FindFieldById(localId);
		if(localField != null) {
			// Add to list so we can catch click events
			localField.setMap(map);
			List<LatLng> points = localField.getBoundary();
			
			// Now draw this field
			// Create polygon
			if(points != null && points.isEmpty() == false) {
				Job theJob = FindJobByFieldName(localField.getName());
				PolygonOptions polygonOptions = new PolygonOptions();
				if (theJob == null || theJob.getStatus() == Job.STATUS_NOT_PLANNED) {
					polygonOptions.fillColor(Field.FILL_COLOR_NOT_PLANNED);
				} else if (theJob.getStatus() == Job.STATUS_PLANNED) {
					polygonOptions.fillColor(Field.FILL_COLOR_PLANNED);
				} else if (theJob.getStatus() == Job.STATUS_STARTED) {
					polygonOptions.fillColor(Field.FILL_COLOR_STARTED);
				} else if (theJob.getStatus() == Job.STATUS_DONE) {
					polygonOptions.fillColor(Field.FILL_COLOR_DONE);
				}
				polygonOptions.strokeWidth(Field.STROKE_WIDTH);
				polygonOptions.strokeColor(Field.STROKE_COLOR);
				for (int i = 0; i < points.size(); i++) {
					polygonOptions.add(points.get(i));
				}
				localField.setPolygon(new MyPolygon(map, map.addPolygon(polygonOptions), this));
				if (currentField != null && localField.getId() == currentField.getId()) {
					this.currentPolygon = localField.getPolygon();
					this.currentPolygon.setLabel(localField.getName(), true);
				} else {
					localField.getPolygon().setLabel(localField.getName());
				}
			}
			FieldsOnMap.add(localField);
		}
		if (this.fragmentListView != null) this.fragmentListView.getData();
	}

	@Override
	public void SyncControllerChangeOrganizations(){
		drawMap();
	}
	*/


	@Override
	public Field AddFieldGetCurrentField() {
		return this.currentField;
	}

	@Override
	public void AddFieldUndo() {
		this.currentPolygon.undo();
	}

	@Override
	public void AddFieldDone(String name, Integer acres) {
		// Check if field name is valid and doesn't exist already
		if (name.length() == 0) {
			// Tell them to input a name
			// TODO add this message to R.strings
			Toast.makeText(this, "Field name cannot be blank.", Toast.LENGTH_LONG).show();
		} else {
			// Check if field name already exists in db
			if (FindFieldByName(name) != null && currentField == null) {
				Toast.makeText(this,
						"A field with this name already exists. Field names must be unique.",
						Toast.LENGTH_LONG).show();
			} else {
				this.currentPolygon.complete();
				this.currentPolygon.setLabel(name, true);
				this.currentPolygon.setFillColor(Field.FILL_COLOR_NOT_PLANNED);
				
				List<LatLng> points = this.currentPolygon.getPoints();
				Boolean wasAnEdit = false;
				if (currentField == null) {
					currentField = new Field(points, map);
				} else {
					currentField.setBoundary(points);
					wasAnEdit = true;
				}
				currentField.setName(name);
				currentField.setAcres(acres);

				Log.d("MainActivity", "Acres:" + Integer.toString(acres));
				String strNewBoundary = "";
				if(points != null && points.isEmpty() == false){
					// Generate boundary
					StringBuilder newBoundary = new StringBuilder(
							points.size() * 20);
					for (int i = 0; i < points.size(); i++) {
						newBoundary.append(points.get(i).latitude);
						newBoundary.append(",");
						newBoundary.append(points.get(i).longitude);
						newBoundary.append(",");
					}
					newBoundary.deleteCharAt(newBoundary.length() - 1);
					strNewBoundary = newBoundary.toString();
				}
				// Save this field to the db
				SQLiteDatabase database = dbHelper.getWritableDatabase();

				ContentValues values = new ContentValues();
				values.put(TableFields.COL_NAME, currentField.getName());
				values.put(TableFields.COL_ACRES, currentField.getAcres());
				values.put(TableFields.COL_BOUNDARY, strNewBoundary);
				
				//TODO only update if something changed
				values.put(TableFields.COL_HAS_CHANGED, 1);
				values.put(TableFields.COL_DATE_CHANGED, DatabaseHelper.dateToStringUTC(new Date()));

				if (wasAnEdit == false) {
					Integer insertId = (int) database.insert(
							TableFields.TABLE_NAME, null, values);
					currentField.setId(insertId);
				} else {
					database.update(
							TableFields.TABLE_NAME,
							values,
							TableFields.COL_ID + " = "
									+ Integer.toString(currentField.getId()),
							null);
				}
				dbHelper.close();

				// Add to list so we can catch click events
				currentField.setPolygon(this.currentPolygon);

				if (wasAnEdit == false) {
					FieldsOnMap.add(currentField);
				} else {
					for (int i = 0; i < FieldsOnMap.size(); i++) {
						if (FieldsOnMap.get(i).getId() == currentField.getId()) {
							FieldsOnMap.get(i).setName(name);
							FieldsOnMap.get(i).setPolygon(this.currentPolygon);
							FieldsOnMap.get(i).setAcres(acres);
							FieldsOnMap.get(i).setBoundary(points);
						}
					}
				}
				
				showSlider(true);
				
				// add or update in list view
				//if (this.fragmentListView != null) this.fragmentListView.getData();
				//this.trelloController.syncDelayed();
			}
		}
	}

	@Override
	public void AddFieldDelete() {
		//Delete the current field
		if(this.currentField != null){
			//Delete field from database
			SQLiteDatabase database = dbHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(TableFields.COL_DELETED, 1);
			values.put(TableFields.COL_HAS_CHANGED, 1);
			values.put(TableFields.COL_DATE_CHANGED, DatabaseHelper.dateToStringUTC(new Date()));
			String where = TableFields.COL_ID + " = "+ Integer.toString(currentField.getId());
			database.update(TableFields.TABLE_NAME, values, where, null);
			
			dbHelper.close();
			for(int i=0; i<FieldsOnMap.size(); i++){
				if(FieldsOnMap.get(i).getId() == currentField.getId()){
					FieldsOnMap.remove(i);
				}
			}
			currentField = null;
			//this.trelloController.syncDelayed();
		}		
		//Remove polygon
		if(this.currentPolygon != null){
			this.currentPolygon.delete();
			this.currentPolygon = null;
		}
		hideAdd(true);
		//if (this.fragmentListView != null) this.fragmentListView.getData();
	}
	
	
	private Field FindFieldByName(String name) {
		if (name != null) {
			SQLiteDatabase database = dbHelper.getReadableDatabase();
			// Find current field
			Field theField = null;
			String where = TableFields.COL_NAME + " = '" + name + "' AND " + TableFields.COL_DELETED + " = 0";
			Cursor cursor = database.query(TableFields.TABLE_NAME,
					TableFields.COLUMNS, where, null, null, null, null);
			if (cursor.moveToFirst()) {
				theField = Field.cursorToField(cursor);
				theField.setMap(map);
			}
			cursor.close();
			dbHelper.close();
			return theField;
		} else {
			return null;
		}
	}

	private Field FindFieldById(Integer id) {
		if (id != null) {
			SQLiteDatabase database = dbHelper.getReadableDatabase();
			// Find current field
			Field theField = null;
			String where = TableFields.COL_ID + " = " + Integer.toString(id) + " AND " + TableFields.COL_DELETED + " = 0";;
			Cursor cursor = database.query(TableFields.TABLE_NAME,
					TableFields.COLUMNS, where, null, null, null, null);
			if (cursor.moveToFirst()) {
				theField = Field.cursorToField(cursor);
				theField.setMap(map);
			}
			cursor.close();
			dbHelper.close();
			return theField;
		} else {
			return null;
		}
	}

	@Override
	public void onMarkerDrag(Marker arg0) {
		if (this.currentPolygon != null) {
			this.currentPolygon.onMarkerDrag(arg0);
		}
	}

	@Override
	public void onMarkerDragEnd(Marker arg0) {
		if (this.currentPolygon != null) {
			this.currentPolygon.onMarkerDragEnd(arg0);
		}
	}

	@Override
	public void onMarkerDragStart(Marker arg0) {
		if (this.currentPolygon != null) {
			this.currentPolygon.onMarkerDragStart(arg0);
		}
	}

	@Override
	public boolean onMarkerClick(Marker arg0) {
		if(this.fragmentSlider == null || this.fragmentSlider.isAddingNote() == false){
			Boolean found = false;
			if (this.currentPolygon != null) {
				found = this.currentPolygon.onMarkerClick(arg0);
			}
			if(found == false){
				this.onMapClick(arg0.getPosition());
			}
		}
		return false;
	}

	@Override
	public void MyPolygonUpdateAcres(Float acres) {
		if(this.fragmentAddField != null){
			this.fragmentAddField.autoAcres(acres);
		}
	}
	
	private void checkGPS(){
		final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
	    if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
	        buildAlertMessageNoGps();
	    }
	}
	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
		               startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
		                dialog.cancel();
		           }
		       });
		final AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}
}

