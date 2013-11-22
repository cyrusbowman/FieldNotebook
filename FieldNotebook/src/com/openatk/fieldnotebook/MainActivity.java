package com.openatk.fieldnotebook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
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
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.TableFields;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolygon.MyPolygonListener;
import com.openatk.fieldnotebook.fieldlist.FieldListListener;
import com.openatk.fieldnotebook.fieldlist.FragmentFieldList;
import com.openatk.fieldnotebook.notelist.FragmentNoteList;
import com.openatk.fieldnotebook.notelist.NoteListListener;
import com.openatk.fieldnotebook.sidebar.FragmentSidebar;
import com.openatk.fieldnotebook.sidebar.SidebarListener;
import com.openatk.fieldnotebook.slider.FragmentSlider;
import com.openatk.fieldnotebook.slider.SliderListener;

public class MainActivity extends FragmentActivity implements OnClickListener,
		OnMapClickListener, OnItemSelectedListener, OnMarkerClickListener, OnMarkerDragListener,
		AddFieldListener, SliderListener, SidebarListener, NoteListListener, FieldListListener, MyPolygonListener {
	
	private static String TAG = MainActivity.class.getName();
	
	private SupportMapFragment fragmentMap;
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
	private int drawingIsShowing = 0;


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult:" + Integer.toString(requestCode));
		if(requestCode == 999){
			Log.d(TAG, "ImageCaptured");
			//Camera
			if(resultCode == RESULT_OK){
				if(this.fragmentNoteList == null) this.getFragmentNoteList();
				if(this.fragmentNoteList != null){
					this.fragmentNoteList.ImageCaptured();
				}
			} else if(resultCode == RESULT_CANCELED){
				Log.d(TAG, "ImageCanceled");
			} else {
				//Failed, TODO toast
				Log.d(TAG, "ImageFailed");
				Toast.makeText(getApplicationContext(), "Image capture failed", Toast.LENGTH_LONG).show();
			}
		}
	}

	private Field currentField = null;
	private MyPolygon currentPolygon = null;

	private List<Field> FieldsOnMap = null;

	String addingBoundary = "";
	
	FragmentAddField fragmentAddField = null;
	FragmentSlider fragmentSlider = null;
	FragmentSidebar fragmentSidebar = null;
	FragmentDrawing fragmentDrawing = null;
	FragmentNoteList fragmentNoteList = null;
	FragmentFieldList fragmentFieldList = null;

	ViewGroup vgSidebar = null;
	
	
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
		fragmentMap = (SupportMapFragment) fm.findFragmentById(R.id.map);
		fragmentSlider = (FragmentSlider) fm.findFragmentByTag(FragmentSlider.class.getName());
		if(fragmentSlider != null){
			sliderIsShowing = 1;
		}
				
		if (savedInstanceState == null) {
			// First incarnation of this activity.
			fragmentMap.setRetainInstance(true);
		} else {
			// Reincarnated activity. The obtained map is the same map instance
			// in the previous
			// activity life cycle. There is no need to reinitialize it.
			map = fragmentMap.getMap();
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
		
		vgSidebar = (ViewGroup) findViewById(R.id.fragment_container_sidebar);
		if (vgSidebar != null) {
			Log.i(TAG, "onCreate: adding FragmentSidebar to MainActivity");

			// Add sidebar fragment to the activity's container layout
			FragmentSidebar fragmentSidebar = new FragmentSidebar();
			FragmentTransaction fragmentTransaction = fm.beginTransaction();
			fragmentTransaction.replace(vgSidebar.getId(), fragmentSidebar, FragmentSidebar.class.getName());

			// Commit the transaction 
			fragmentTransaction.commit();
		}

		setUpMapIfNeeded();
		
		Intent intent = this.getIntent();
		String todo = intent.getStringExtra("todo");
		if(todo != null){
			if(todo.contentEquals("sync")){
				//trelloController.sync();
			}
		}
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
		if(this.fragmentNoteList == null){
			this.fragmentNoteList = this.getFragmentNoteList();
		}
		
		if (addIsShowing == 1 || addingNotePolygon) {
			Log.d("HERE", "Here1");
			// Add points to polygon
			this.currentPolygon.addPoint(position);
		} else if(this.fragmentNoteList != null && this.fragmentNoteList.isAddingNote() == true) {
			//Adding a note, give the note the click events
			this.fragmentNoteList.onMapClick(position);
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
							this.SliderRequestData(null); //Populate slider again
							this.SidebarRequestData(null);
							this.NoteListRequestData(null); //Populate note list again
						}
					}
					showSlider(true);
					break;
				}
			}
			if (touched == false) {
				// Close menu, save edits
				Log.d("MainActivity - onMapClick", "Close");
				hideSlider(true);
				ExitField();
			}
		}
	}
	
	private void ExitField(){
		if (this.currentPolygon != null) {
			// Set back to unselected
			this.currentPolygon.unselect();
		}
		this.currentField = null;
		if(this.fragmentNoteList == null) this.fragmentNoteList = this.getFragmentNoteList();
		if(this.fragmentSidebar == null) this.fragmentSidebar = this.getFragmentSidebar();
		if(this.fragmentFieldList == null) this.fragmentFieldList = this.getFragmentFieldList();
		
		if(this.fragmentNoteList != null) this.fragmentNoteList.onClose();
		if(this.fragmentSidebar != null) this.fragmentSidebar.populateData(null, this.fragmentMap.getView());
		if(this.fragmentFieldList != null) this.fragmentFieldList.populateData(null);
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

	private Void showDrawing(Boolean transition) {
		if (drawingIsShowing == 0) {
			drawingIsShowing = 1;
			// Set height back to wrap, in case add buttons or something
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_drawing);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
			params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			layout.setLayoutParams(params);

			FragmentManager fm = getSupportFragmentManager();
			FragmentDrawing fragment = new FragmentDrawing();
			FragmentTransaction ft = fm.beginTransaction();
			if (transition) ft.setCustomAnimations(R.anim.slide_down2, R.anim.slide_up2);
			ft.add(R.id.fragment_container_drawing, fragment, "drawing");
			ft.commit();
			fragmentDrawing = fragment;
		}
		this.invalidateOptionsMenu();
		return null;
	}

	private void hideDrawing(Boolean transition) {
		if (drawingIsShowing == 1) {
			drawingIsShowing = 0;
			FragmentManager fm = getSupportFragmentManager();
			FragmentDrawing fragment = (FragmentDrawing) fm.findFragmentByTag("drawing");
			// Set height so transition works
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_drawing);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
					.getLayoutParams();
			params.height = fragment.getHeight();
			layout.setLayoutParams(params);
			// Do transition
			FragmentTransaction ft = fm.beginTransaction();
			if (transition) ft.setCustomAnimations(R.anim.slide_down2, R.anim.slide_up2);
			ft.remove(fragment);
			ft.commit();
			fragmentDrawing = null;
		}
		this.invalidateOptionsMenu();
	}	

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
			if (transition) ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
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
			if (transition) ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
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
			if(layout != null){
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
				params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
				layout.setLayoutParams(params);
				FragmentManager fm = getSupportFragmentManager();
				this.fragmentSlider = new FragmentSlider();
				FragmentTransaction ft = fm.beginTransaction();
				if (transition) ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
				ft.add(R.id.fragment_container_slider, this.fragmentSlider, FragmentSlider.class.getName());
				ft.commit();
				Log.d("MainActivity", "Showing Slider:" + FragmentSlider.class.getName());
			}
		}
		this.invalidateOptionsMenu();
		return null;
	}

	private void hideSlider(Boolean transition) {
		if (sliderIsShowing == 1) {
			sliderIsShowing = 0;
			if(fragmentNoteList == null){
				this.fragmentNoteList = this.getFragmentNoteList();
			}
			if(fragmentNoteList != null) fragmentNoteList.onClose();
			
			FragmentManager fm = getSupportFragmentManager();
			FragmentSlider fragment = (FragmentSlider) fm.findFragmentByTag(FragmentSlider.class.getName());
			// Set height so transition works TODO 3 different heights?? Get from fragment, fragment.getMyHeight?
			FrameLayout layout = (FrameLayout) findViewById(R.id.fragment_container_slider);
			if(layout != null){
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout
						.getLayoutParams();
				params.height = fragment.getHeight();
				layout.setLayoutParams(params);
				// Do transition
				FragmentTransaction ft = fm.beginTransaction();
				if (transition) ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
				ft.remove(fragment);
				ft.commit();
			}
			fragmentSlider = null;
		}
		this.invalidateOptionsMenu();
	}	
	
	
	
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
		if(this.fragmentNoteList == null){
			this.fragmentNoteList = this.getFragmentNoteList();
		}
		if(this.fragmentNoteList == null || this.fragmentNoteList.isAddingNote() == false){
			Boolean found = false;
			if (this.currentPolygon != null) {
				found = this.currentPolygon.onMarkerClick(arg0);
			}
			if(found == false){
				this.onMapClick(arg0.getPosition());
			}
		}
		return true;
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
	
	// --------------------------------- FragmentNoteList ----------------------------------
	private MyPolygon saveFieldPolygon = null;
	private Boolean addingNotePolygon = false;
	private FragmentNoteList getFragmentNoteList(){
		FragmentManager fm = getSupportFragmentManager();
		return (FragmentNoteList) fm.findFragmentByTag(FragmentNoteList.class.getName());
	}
	
	@Override
	public void NoteListAddPolygon() {
		// Add note polygon
		addingNotePolygon = true;
		saveFieldPolygon = this.currentPolygon;
		this.currentPolygon = new MyPolygon(map, this);
	}
	
	@Override
	public void NoteListEditPolygon(MyPolygon poly) {
		// Add note polygon
		addingNotePolygon = true;
		saveFieldPolygon = this.currentPolygon;
		this.currentPolygon = poly;
	}
	
	@Override
	public void NoteListCompletePolygon(){
		addingNotePolygon = false;
		this.currentPolygon.complete();
		//this.currentPolygon.setLabel(name, true);
		this.currentPolygon.setFillColor(Field.FILL_COLOR_PLANNED);
		if(this.fragmentNoteList == null){
			this.fragmentNoteList = this.getFragmentNoteList();
		}
		if(this.fragmentNoteList != null){
			this.fragmentNoteList.finishPolygon(this.currentPolygon);
		}
		this.currentPolygon = saveFieldPolygon;
		saveFieldPolygon = null;
	}
	
	@Override
	public void NoteListRequestData(FragmentNoteList requester) {
		if(requester != null) this.fragmentNoteList = requester;
		if(this.fragmentNoteList == null){
			this.fragmentNoteList = this.getFragmentNoteList();
		}
		if(this.fragmentNoteList != null){
			Integer id = null;
			if(this.currentField != null) id = this.currentField.getId();
			this.fragmentNoteList.populateData(id, map);
		} else {
			Log.d("MainActivity", "this.fragmentNoteList is null");
		}
	}

	@Override
	public FragmentDrawing NoteListShowDrawing() {
		showDrawing(true);
		return this.fragmentDrawing;
	}

	@Override
	public void NoteListHideDrawing() {
		hideDrawing(true);		
	}

	@Override
	public void NoteListAddNote() {
		
	}
	
	// ----------------------------- FragmentSlider -------------------------------

	private FragmentSlider getFragmentSlider(){
		FragmentManager fm = getSupportFragmentManager();
		return (FragmentSlider) fm.findFragmentByTag(FragmentSlider.class.getName());
	}

	@Override
	public void SliderAddNote() {
		Log.d("MainActivity", "Slider Add Note");
		//TODO Resize to oneNote Size
		if(this.fragmentNoteList == null){
			this.fragmentNoteList = this.getFragmentNoteList();
		}
		if(this.fragmentSlider == null){
			this.fragmentSlider = this.getFragmentSlider();
		}
		if(this.fragmentNoteList != null){
			Boolean addNote = this.fragmentNoteList.AddNote();
			if(addNote && this.fragmentSlider != null){
				this.fragmentSlider.SliderSizeMiddle();
			}
		}
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

	@Override
	public void SliderRequestData(FragmentSlider requester) {
		if(requester != null) this.fragmentSlider = requester;
		if(this.fragmentSlider == null){
			this.fragmentSlider = this.getFragmentSlider();
		}
		if(this.fragmentSlider != null){
			this.fragmentSlider.populateData(this.currentField, this.fragmentMap.getView());
		} else {
			Log.d("MainActivity", "this.fragmentSlider is null");
		}
	}

	// ----------------------------- FragmentSidebar -------------------------------
	private FragmentSidebar getFragmentSidebar(){
		FragmentManager fm = getSupportFragmentManager();
		return (FragmentSidebar) fm.findFragmentByTag(FragmentSidebar.class.getName());
	}
	
	@Override
	public void SidebarAddNote() {
		Log.d("MainActivity", "Sidebar Add Note");
		//TODO Resize to oneNote Size
		if(this.fragmentNoteList == null){
			this.fragmentNoteList = this.getFragmentNoteList();
		}
		if(this.fragmentSidebar == null){
			this.fragmentSidebar = this.getFragmentSidebar();
		}
		if(this.fragmentNoteList != null){
			Boolean addNote = this.fragmentNoteList.AddNote();
			if(addNote == false){
				Log.d("Mainactivty", "Not adding note");
			}
		}
	}
	
	@Override
	public void SidebarAddField() {
		this.addFieldMapView();
	}

	@Override
	public void SidebarRequestData(FragmentSidebar requester) {
		//TODO get container to set width, and have the slide work
		if(requester != null) this.fragmentSidebar = requester;
		if(this.fragmentSidebar == null){
			this.fragmentSidebar = this.getFragmentSidebar();
		}
		if(this.fragmentSidebar != null){
			this.fragmentSidebar.populateData(this.currentField, this.fragmentMap.getView());
		} else {
			Log.d("MainActivity", "this.fragmentSidebar is null");
		}
	}
	
	@Override
	public void SidebarEditField() {
		this.SliderEditField();
	}
	
	@Override
	public void SidebarBackToFieldsList() {
		if(this.fragmentSidebar == null){
			this.fragmentSidebar = this.getFragmentSidebar();
		}
		ExitField();
	}
	
	// ----------------------------- FragmentFieldList -------------------------------
	private FragmentFieldList getFragmentFieldList(){
		FragmentManager fm = getSupportFragmentManager();
		return (FragmentFieldList) fm.findFragmentByTag(FragmentFieldList.class.getName());
	}
	
	@Override
	public void FieldListRequestData(FragmentFieldList requester) {
		if(requester != null) this.fragmentFieldList = requester;
		if(this.fragmentFieldList == null){
			this.fragmentFieldList = this.getFragmentFieldList();
		}
		if(this.fragmentFieldList != null){
			Integer id = null;
			if(this.currentField != null) id = this.currentField.getId();
			this.fragmentFieldList.populateData(id);
		} else {
			Log.d("MainActivity", "this.fragmentFieldList is null");
		}
	}

	@Override
	public void FieldListAddNote() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void FieldListSelectField(Field selectedField) {
		this.currentField = selectedField;
		this.SidebarRequestData(null);
		this.NoteListRequestData(null); //Populate notes again
	}


}

