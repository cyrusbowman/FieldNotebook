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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.db.Field;

public class MyPolyline implements OnMapClickListener, OnMarkerDragListener, OnMarkerClickListener {

	private static double TOUCH_DISTANCE = 20.0f;
	private static float STROKE_WIDTH = 2.0f;
	private static int STROKE_COLOR = Color.BLACK;
	private static int FILL_COLOR = Color.argb(128, 74, 80, 255);
	
	private Polyline real = null;
	private GoogleMap map = null;
	private Object data = null;
	private List<Marker> markers;
	
	private BitmapDescriptor iconSelected;
	private BitmapDescriptor icon;
	
	private LatLngBounds bounds;
	
	
	private Integer markerSelected = null;

	public MyPolyline(GoogleMap map) {
		this.map = map;
		this.markers = new ArrayList<Marker>();
		iconSelected = BitmapDescriptorFactory.fromResource(R.drawable.selected_vertex);
		icon = BitmapDescriptorFactory.fromResource(R.drawable.unselected_vertex);
	}
	
	public MyPolyline(Polyline real, GoogleMap map) {
		this.real = real;
		this.map = map;
		this.markers = new ArrayList<Marker>();
		iconSelected = BitmapDescriptorFactory.fromResource(R.drawable.selected_vertex);
		icon = BitmapDescriptorFactory.fromResource(R.drawable.unselected_vertex);
		
		//Make bounds for polyline
		List<LatLng> arrayLoc = this.real.getPoints();
		updateBounds(arrayLoc);
	}

	//Ability to move and edit polyline
	//Try to implement handle?? Should it be on screen or map? Try on screen first
	//Make bounds around polyline, implement select as distance from point to line length
	
	//My Methods
	// Custom functions
	public void select() {
		//setStrokeColor(Field.STROKE_SELECTED);
	}
	public void unselect() {
		//setStrokeColor(Field.STROKE_COLOR);
	}
	public void undo() {
		// Remove selected marker
		if (this.markers.size() != 0) {
			this.markers.get(markerSelected.intValue()).remove();
			this.markers.remove(markerSelected.intValue());
			Integer newSelect = null;
			if (this.markers.size() != 0) {
				newSelect = markerSelected.intValue() - 1;
				if (newSelect < 0) {
					newSelect = 0;
				}
			}
			markerSelected = null;
			if (newSelect != null) {
				selectMarker(newSelect);
			}
			updateShape();
		}
	}

	public void complete() {
		// Remove all markers, and maybe change fill?
		for (int i = 0; i < this.markers.size(); i++) {
			this.markers.get(i).remove();
		}
		this.markers.clear();
		markerSelected = null;
	}

	public void edit() {
		map.setOnMapClickListener(this);
		map.setOnMarkerClickListener(this);
		map.setOnMarkerDragListener(this);
		if (real != null) {
			List<LatLng> points = real.getPoints();
			// Draw markers
			for (int i = 0; i < points.size(); i++) {
				this.markers.add(map.addMarker(new MarkerOptions().position(points.get(i)).icon(icon).draggable(true).anchor(0.5f, 0.5f)));
			}
			if (this.markers.size() > 0) selectMarker(this.markers.size() - 1);
		}
		updateShape();
	}

	@Override
	public void onMarkerDrag(Marker marker) {
		updateShape();
	}
	@Override
	public void onMarkerDragEnd(Marker marker) {
		updateShape();
		onMarkerClick(marker);
	}
	@Override
	public void onMarkerDragStart(Marker marker) {
		
	}
	@Override
	public boolean onMarkerClick(Marker marker) {
		Integer markerIndex = null;
		for (int i = 0; i < markers.size(); i++) {
			if (markers.get(i).equals(marker)) {
				markerIndex = i;
				break;
			}
		}
		if(markerIndex != null){
			selectMarker(markerIndex);
			return true;
		} else {
			this.onMapClick(marker.getPosition());
			return true; //Consume click stop autocenter, only our markers clickable
		}
	}
	@Override
	public void onMapClick(LatLng arg0) {
		addPoint(arg0);
	}

	public void delete() {
		this.remove();
		if(this.markers != null){
			for (int i = 0; i < this.markers.size(); i++) {
				this.markers.get(i).remove();
			}
			this.markers.clear();
		}
	}

	private void selectMarker(Integer markerIndex) {
		if (markerSelected != null) {
			Log.d("MarkerSelected:", Integer.toString(markerSelected));
			// Change icon of last selected marker
			Marker oldMarker = markers.get(markerSelected.intValue());
			oldMarker.setIcon(icon);
		}
		if (markerIndex != null) {
			Log.d("MarkerIndex:", Integer.toString(markerIndex));
			// Change icon on new selected marker
			Marker oldMarker = this.markers.get(markerIndex.intValue());
			oldMarker.setIcon(iconSelected);
		}
		markerSelected = markerIndex;
	}

	public void addPoint(LatLng point) {
		// Add marker
		int location = this.markers.size();
		if (markerSelected != null) location = markerSelected.intValue() + 1;
		this.markers.add(location,map.addMarker(new MarkerOptions().position(point).icon(icon).draggable(true).anchor(0.5f, 0.5f)));
		Log.d("MarkersSize:", Integer.toString(this.markers.size()));
		selectMarker(location);
		updateShape();
	}

	public void updatePoints(List<LatLng> arrayLoc){
		if(arrayLoc.size() < 2){
			if (real != null) {
				// remove polyline
				real.remove();
				real = null;
			}
		} if(arrayLoc.size() >= 2) {
			if (real == null) {
				// Create polyline
				PolylineOptions lineOptions = new PolylineOptions();
				lineOptions.color(STROKE_COLOR);
				lineOptions.width(STROKE_WIDTH);
				lineOptions.zIndex(2.0f);
				for (int i = 0; i < arrayLoc.size(); i++) {
					lineOptions.add(arrayLoc.get(i));
				}				
				// Get back the mutable Polyline
				real = map.addPolyline(lineOptions);
			} else {
				// Update polyline
				real.setPoints(arrayLoc);
			}
		}
		updateBounds(arrayLoc); //Update the bounding box
	}
	
	private void updateBounds(List<LatLng> arrayLoc){
		if(arrayLoc != null && arrayLoc.size() >= 2) {
			LatLngBounds.Builder builder = new LatLngBounds.Builder();
			for(int i=0; i<arrayLoc.size(); i++){
				builder.include(arrayLoc.get(i));
			}
			this.bounds = builder.build();
			//Pad a little
			Projection proj = map.getProjection();
			Point swPoint = proj.toScreenLocation(this.bounds.southwest);
			Point nePoint = proj.toScreenLocation(this.bounds.northeast);
			nePoint.x = nePoint.x + 10;
			nePoint.y = nePoint.y + 10;
			swPoint.x = swPoint.x - 10;
			swPoint.y = swPoint.y - 10;
			builder.include(proj.fromScreenLocation(nePoint));
			builder.include(proj.fromScreenLocation(swPoint));
			//Make new padded bounds
			this.bounds = builder.build();
			Log.d("Built polyline...", "Bounds built");
		} else {
			this.bounds = null;
		}
	}
	
	public void updateShape() {
		List<LatLng> arrayLoc = new ArrayList<LatLng>();
		// Create polyline or update polyline
		for (int i = 0; i < this.markers.size(); i++) {
			Marker curMarker = this.markers.get(i);
			LatLng curPos = curMarker.getPosition();
			arrayLoc.add(curPos);
		}
		updatePoints(arrayLoc); //Updates polyline to new shape after marker move
	}
	
	
	public Boolean wasTouched(LatLng point) {
		if(this.bounds == null){
			Log.d("Null Bounds", "Null bounds");
		}
		if(this.real != null && this.bounds != null){
			Log.d("wasTouched", "Here1");
			if(bounds.contains(point)){
				Log.d("wasTouched", "Here2");

				List<LatLng> points = this.real.getPoints();
				// Convert to screen coordinate
				Projection proj = map.getProjection();
				Point touchPoint = proj.toScreenLocation(point);

				// Convert boundary to screen coordinate
				List<Point> polyline = new ArrayList<Point>();
				for (int i = 0; i < points.size(); i++) {
					polyline.add(proj.toScreenLocation(points.get(i)));
				}
				
				// Ray Cast
				return isPointByPolyline(touchPoint, polyline);
			} else {
				return false;
			}
		}
		return false;
	}
	
	private boolean isPointByPolyline(Point touchPoint, List<Point> polyline){
		for(int i=0; i<(polyline.size()-1); i++){
			Point a = polyline.get(i);
			Point b = polyline.get(i+1);
			Double touchDistance = pointToLineDistance(a,b,touchPoint);
			Log.d("TouchDistance:", Double.toString(touchDistance));
			if(touchDistance < TOUCH_DISTANCE){
				return true;
			}
		}
		return false;
	}
	
	 private double pointToLineDistance(Point A, Point B, Point P) {
		 //From http://www.ahristov.com/tutorial/geometry-games/point-line-distance.html
		 double normalLength = Math.sqrt((B.x-A.x)*(B.x-A.x)+(B.y-A.y)*(B.y-A.y));
		 return Math.abs((P.x-A.x)*(B.y-A.y)-(P.y-A.y)*(B.x-A.x))/normalLength;
	 }
	 
	//Google Polyline methods
	public int getColor() {
		return real.getColor();
	}

	public Object getData() {
		return data;
	}

	@Deprecated
	public String getId() {
		return real.getId();
	}

	public List<LatLng> getPoints() {
		return real.getPoints();
	}

	public float getWidth() {
		return real.getWidth();
	}

	public float getZIndex() {
		return real.getZIndex();
	}

	public boolean isGeodesic() {
		return real.isGeodesic();
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public void remove() {
		real.remove();
	}

	public void setColor(int color) {
		real.setColor(color);
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void setGeodesic(boolean geodesic) {
		real.setGeodesic(geodesic);
	}

	public void setPoints(List<LatLng> points) {
		real.setPoints(points);
	}

	public void setVisible(boolean visible) {
		real.setVisible(visible);
	}

	public void setWidth(float width) {
		real.setWidth(width);
	}

	public void setZIndex(float zIndex) {
		real.setZIndex(zIndex);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MyPolyline)) {
			return false;
		}
		MyPolyline other = (MyPolyline) o;
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
