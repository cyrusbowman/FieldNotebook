package com.openatk.fieldnotebook.drawing;

public class AtkPoint {
	private double lat;
	private double lng;
	
	public AtkPoint(){
		
	}
	public AtkPoint(double lat, double lng){
		this.lat = lat;
		this.lng = lng;
	}
	public AtkPoint(String LatLng){
		
	}
	
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	public String getStrLatLng(){
		return Double.toString(lat) + ',' + Double.toString(lng);
	}

	
}
