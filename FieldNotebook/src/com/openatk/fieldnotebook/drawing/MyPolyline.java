package com.openatk.fieldnotebook.drawing;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

public class MyPolyline {

	private Polyline real;
	private GoogleMap map;
	private Object data;

	MyPolyline(Polyline real, GoogleMap map) {
		this.real = real;
		this.map = map;
	}

	//Ability to move and edit polyline
	//Try to implement handle?? Should it be on screen or map? Try on screen first
	
	
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
