package com.openatk.fieldnotebook.drawing;

import java.util.List;

public class AtkPolyline {
	private List<AtkPoint> listPoints;
	
	public AtkPolyline(){
		
	}
	public AtkPolyline(List<AtkPoint> listPoints){
		this.listPoints = listPoints;
	}
	
	public void addPoint(AtkPoint atkPoint){
		this.listPoints.add(atkPoint);
	}
	public void addPoint(int location, AtkPoint atkPoint){
		this.listPoints.add(location, atkPoint);
	}
	public void removePoint(AtkPoint atkPoint){
		this.listPoints.remove(atkPoint);
	}
	public void removePoint(int location){
		this.listPoints.remove(location);
	}
	public List<AtkPoint> getPoints(){
		return this.listPoints;
	}
	public void setPoints(List<AtkPoint> listPoints){
		this.listPoints = listPoints;
	}
	public String getStrPoints(){
		if(this.listPoints != null && this.listPoints.isEmpty() == false){
			// Generate boundary
			StringBuilder buildString = new StringBuilder(this.listPoints.size() * 20);
			for (int j = 0; j < this.listPoints.size(); j++) {
				buildString.append(Double.toString(this.listPoints.get(j).getLat()));
				buildString.append(",");
				buildString.append(Double.toString(this.listPoints.get(j).getLng()));
				buildString.append(",");
			}
			if(buildString.length() > 0){
				buildString.deleteCharAt(buildString.length() - 1);
			}
			return buildString.toString();
		}
		return null;
	}
}
