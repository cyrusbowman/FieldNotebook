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

public class AtkMapPolylineView implements OnMapClickListener, OnMarkerDragListener, OnMarkerClickListener {

	private static float STROKE_WIDTH = 2.0f;
	private static int STROKE_COLOR = Color.BLACK;
	private static int FILL_COLOR = Color.argb(128, 74, 80, 255);
	
	
	AtkPolyline model = null;

	
	private GoogleMap map = null;
	private Polyline polyline = null;
	private List<Marker> markers;
	private Object data = null;
	
	
	private BitmapDescriptor iconSelected;
	private BitmapDescriptor icon;
	
	private Integer markerSelected = null;

	public AtkMapPolylineView(GoogleMap map) {
		this.map = map;
		this.markers = new ArrayList<Marker>();
		iconSelected = BitmapDescriptorFactory.fromResource(R.drawable.selected_vertex);
		icon = BitmapDescriptorFactory.fromResource(R.drawable.unselected_vertex);
	}
	public AtkMapPolylineView(AtkPolyline model, GoogleMap map) {
		this.model = model;
		this.map = map;
		this.markers = new ArrayList<Marker>();
		iconSelected = BitmapDescriptorFactory.fromResource(R.drawable.selected_vertex);
		icon = BitmapDescriptorFactory.fromResource(R.drawable.unselected_vertex);
	}

	//Ability to move and edit polyline
	//Try to implement handle?? Should it be on screen or map? Try on screen first
	//Make bounds around polyline, implement select as distance from point to line length
	
	private void draw(){
		List<AtkPoint> points = this.model.getPoints();
		if(points.size() < 2){
			if (this.polyline != null) {
				// remove polyline
				this.polyline.remove();
				this.polyline = null;
			}
		} if(points.size() >= 2) {
			if (this.polyline == null) {
				// Create polyline
				PolylineOptions lineOptions = new PolylineOptions();
				lineOptions.color(STROKE_COLOR);
				lineOptions.width(STROKE_WIDTH);
				lineOptions.zIndex(2.0f);
				for (int i = 0; i < points.size(); i++) {
					LatLng newLatLng = new LatLng(points.get(i).getLat(), points.get(i).getLng());
					lineOptions.add(newLatLng);
				}
				this.polyline = map.addPolyline(lineOptions);
			} else {
				// Update polyline
				List<LatLng> latLngPoints = new ArrayList<LatLng>();
				for (int i = 0; i < points.size(); i++) {
					LatLng newLatLng = new LatLng(points.get(i).getLat(), points.get(i).getLng());
					latLngPoints.add(newLatLng);
				}
				this.polyline.setPoints(latLngPoints);
			}
		}
	}
	
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
		if (polyline != null) {
			List<LatLng> points = polyline.getPoints();
			// Draw markers
			for (int i = 0; i < (points.size() - 1); i++) {
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
			if (polyline != null) {
				// remove polyline
				polyline.remove();
				polyline = null;
			}
		} if(arrayLoc.size() >= 2) {
			if (polyline == null) {
				// Create polyline
				PolylineOptions lineOptions = new PolylineOptions();
				lineOptions.color(STROKE_COLOR);
				lineOptions.width(STROKE_WIDTH);
				lineOptions.zIndex(2.0f);
				for (int i = 0; i < arrayLoc.size(); i++) {
					lineOptions.add(arrayLoc.get(i));
				}
				// Get back the mutable Polyline
				polyline = map.addPolyline(lineOptions);
			} else {
				// Update polyline
				polyline.setPoints(arrayLoc);
			}
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
		//TODO bounding box?
		if(this.polyline != null){
			/*List<LatLng> points = this.real.getPoints();
			// Convert to screen coordinate
			Projection proj = map.getProjection();
			Point touchPoint = proj.toScreenLocation(point);

			// Convert boundary to screen coordinate
			List<Point> boundaryPoints = new ArrayList<Point>();
			for (int i = 0; i < points.size(); i++) {
				boundaryPoints.add(proj.toScreenLocation(points.get(i)));
			}

			// Ray Cast
			return isPointInPolygon(touchPoint, boundaryPoints);*/
		}
		return false;
	}
	
	
	//Google Polyline methods
	public int getColor() {
		return polyline.getColor();
	}

	public Object getData() {
		return data;
	}

	@Deprecated
	public String getId() {
		return polyline.getId();
	}

	public List<LatLng> getPoints() {
		return polyline.getPoints();
	}

	public float getWidth() {
		return polyline.getWidth();
	}

	public float getZIndex() {
		return polyline.getZIndex();
	}

	public boolean isGeodesic() {
		return polyline.isGeodesic();
	}

	public boolean isVisible() {
		return polyline.isVisible();
	}

	public void remove() {
		polyline.remove();
	}

	public void setColor(int color) {
		polyline.setColor(color);
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void setGeodesic(boolean geodesic) {
		polyline.setGeodesic(geodesic);
	}

	public void setPoints(List<LatLng> points) {
		polyline.setPoints(points);
	}

	public void setVisible(boolean visible) {
		polyline.setVisible(visible);
	}

	public void setWidth(float width) {
		polyline.setWidth(width);
	}

	public void setZIndex(float zIndex) {
		polyline.setZIndex(zIndex);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AtkMapPolylineView)) {
			return false;
		}
		AtkMapPolylineView other = (AtkMapPolylineView) o;
		return polyline.equals(other.polyline);
	}

	@Override
	public int hashCode() {
		return polyline.hashCode();
	}

	@Override
	public String toString() {
		return polyline.toString();
	}
}
