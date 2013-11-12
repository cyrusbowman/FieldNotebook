package com.openatk.fieldnotebook.drawing;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.db.Field;

public class MyMarker {

	private Marker real = null;
	private GoogleMap map = null;
	private Object data = null;
	
	private BitmapDescriptor iconSelected;
	private BitmapDescriptor icon;
	
	public MyMarker(GoogleMap map, LatLng loc) {
		this.map = map;
		//iconSelected = BitmapDescriptorFactory.fromResource(R.drawable.selected_vertex);
		//icon = BitmapDescriptorFactory.fromResource(R.drawable.unselected_vertex);
		iconSelected = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
		icon  = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
		real = map.addMarker(new MarkerOptions().position(loc).icon(iconSelected).draggable(true).anchor(0.5f, 0.5f));
	}
	
	public MyMarker(Marker real, GoogleMap map) {
		this.real = real;
		this.map = map;
		iconSelected = BitmapDescriptorFactory.fromResource(R.drawable.selected_vertex);
		icon = BitmapDescriptorFactory.fromResource(R.drawable.unselected_vertex);
	}
	
	//My Methods
	// Custom functions
	public void select() {
		this.real.setIcon(iconSelected);
	}
	public void unselect() {
		this.real.setIcon(icon);
	}

	public void delete() {
		this.remove();
	}

	//Google Polyline methods
	public LatLng getPosition() {
		return real.getPosition();
	}

	public Object getData() {
		return data;
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public void remove() {
		real.remove();
	}


	public void setData(Object data) {
		this.data = data;
	}

	public void setColor(int color){
		if(color == Color.RED){
			real.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
		} else if(color == Color.YELLOW){
			real.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
		} else if(color == Color.BLUE){
			real.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
		} else if(color == Color.GREEN){
			real.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		}
	}
	
	public void setVisible(boolean visible) {
		real.setVisible(visible);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MyMarker)) {
			return false;
		}
		MyMarker other = (MyMarker) o;
		return real.equals(other.real);
	}

	@Override
	public int hashCode() {
		return real.hashCode();
	}

	@Override
	public String toString() {
		return real.toString();
	}
}
