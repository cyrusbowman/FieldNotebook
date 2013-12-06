package com.openatk.fieldnotebook;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

public class FragmentDrawing extends Fragment implements OnClickListener {

	private DrawingListener listener = null;
	private ImageButton butPoint = null;
	private ImageButton butPolyline = null;
	private ImageButton butPolygon = null;
	private ImageButton butColor = null;
	private ImageButton butCamera = null;

	// Interface for receiving data
	public interface DrawingListener {
		public void DrawingClickPoint();
		public void DrawingClickPolyline();
		public void DrawingClickPolygon();
		public void DrawingClickColor();
		public void DrawingClickCamera();
		public void DrawingDelete();
		public void DrawingUndo();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
	    inflater.inflate(R.menu.drawing, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_drawing_delete) {
			listener.DrawingDelete();
			return true;
		} else if (item.getItemId() == R.id.menu_drawing_undo) {
			listener.DrawingUndo();
			return true;
		} else if(item.getItemId() == R.id.main_menu_legal){
			/*CharSequence licence= "The MIT License (MIT)\n" +
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
				.setPositiveButton("Close", null).show();*/
		}
		return super.onOptionsItemSelected(item);
	}

	public void setPointIcon(int resId, boolean active){
		this.butPoint.setImageResource(resId);
		if(active){
			this.butPolyline.setEnabled(false);
			this.butPolygon.setEnabled(false);
			this.butColor.setEnabled(false);
			this.butCamera.setEnabled(false);
		} else {
			this.butPolyline.setEnabled(true);
			this.butPolygon.setEnabled(true);
			this.butColor.setEnabled(true);
			this.butCamera.setEnabled(true);
		}
	}
	public void setPolylineIcon(int resId, boolean active){
		this.butPolyline.setImageResource(resId);
		if(active){
			this.butPoint.setEnabled(false);
			this.butPolygon.setEnabled(false);
			this.butColor.setEnabled(false);
			this.butCamera.setEnabled(false);
		} else {
			this.butPoint.setEnabled(true);
			this.butPolygon.setEnabled(true);
			this.butColor.setEnabled(true);
			this.butCamera.setEnabled(true);
		}
	}
	public void setPolygonIcon(int resId, boolean active){
		this.butPolygon.setImageResource(resId);
		if(active){
			this.butPolyline.setEnabled(false);
			this.butPoint.setEnabled(false);
			this.butColor.setEnabled(false);
			this.butCamera.setEnabled(false);
		} else {
			this.butPolyline.setEnabled(true);
			this.butPoint.setEnabled(true);
			this.butColor.setEnabled(true);
			this.butCamera.setEnabled(true);
		}
	}
	public void setColorIcon(int resId, boolean active){
		this.butColor.setImageResource(resId);
		if(active){
			this.butPolyline.setEnabled(false);
			this.butPolygon.setEnabled(false);
			this.butPoint.setEnabled(false);
			this.butCamera.setEnabled(false);
		} else {
			this.butPolyline.setEnabled(true);
			this.butPolygon.setEnabled(true);
			this.butPoint.setEnabled(true);
			this.butCamera.setEnabled(true);
		}
	}
	public void setCameraIcon(int resId, boolean active){
		this.butCamera.setImageResource(resId);
		if(active){
			this.butPolyline.setEnabled(false);
			this.butPolygon.setEnabled(false);
			this.butColor.setEnabled(false);
			this.butPoint.setEnabled(false);
		} else {
			this.butPolyline.setEnabled(true);
			this.butPolygon.setEnabled(true);
			this.butColor.setEnabled(true);
			this.butPoint.setEnabled(true);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_drawing, container, false);

		butPoint = (ImageButton) view.findViewById(R.id.fragment_drawing_butPoint);
		butPolyline = (ImageButton) view.findViewById(R.id.fragment_drawing_butLine);
		butPolygon = (ImageButton) view.findViewById(R.id.fragment_drawing_butPolygon);
		butColor = (ImageButton) view.findViewById(R.id.fragment_drawing_butColor);
		butCamera = (ImageButton) view.findViewById(R.id.fragment_drawing_butPicture);

		butPoint.setOnClickListener(this);
		butPolyline.setOnClickListener(this);
		butPolygon.setOnClickListener(this);
		butColor.setOnClickListener(this);
		butCamera.setOnClickListener(this);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("FragmentDrawing", "Attached");
	}
	
	public void setListener(DrawingListener listener){
		this.listener = listener;
	}

	public int getHeight() {
		// Method so close transition can work
		return getView().getHeight();
	}

	@Override
	public void onClick(View v) {
		if(listener != null){
			if (v.getId() == R.id.fragment_drawing_butPoint) {
				listener.DrawingClickPoint();
			} else if (v.getId() == R.id.fragment_drawing_butLine) {
				listener.DrawingClickPolyline();
			} else if (v.getId() == R.id.fragment_drawing_butPolygon) {
				listener.DrawingClickPolygon();
			} else if (v.getId() == R.id.fragment_drawing_butColor) {
				listener.DrawingClickColor();
			} else if (v.getId() == R.id.fragment_drawing_butPicture) {
				listener.DrawingClickCamera();
			}
		}
	}
}
