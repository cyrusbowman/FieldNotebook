package com.openatk.fieldnotebook.db;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.openatk.fieldnotebook.drawing.MyPolygon;

public class Note {
	private Integer id = null;
	private String remote_id = null;
	private Integer hasChanged = 0;
	private String dateChanged = null;
	private String fieldName = null;
	private String comment = null;
	private String strPolygons = null;
	private List<MyPolygon> myPolygons = new ArrayList<MyPolygon>();
	private List<PolygonOptions> polygons = null;
	//private List<MyLine> lines;
	//private List<MyPoint> points;
	private Integer color = null;
	private Integer visible = 0;
	private Integer deleted = 0;
	
	public Note(){
		
	}
	public Note(String fieldName){
		this.fieldName = fieldName;
	}
	
	public void addMyPolygon(MyPolygon polygon){
		this.myPolygons.add(polygon);
	}
	public void myPolygonsToStringPolygons(){
		StringBuilder buildNewPolygons = new StringBuilder();
		for(int i=0; i<myPolygons.size(); i++){
			List<LatLng> points = myPolygons.get(i).getPoints();
			if(points != null && points.isEmpty() == false){
				// Generate boundary
				StringBuilder newBoundary = new StringBuilder(points.size() * 20);
				for (int j = 0; j < points.size(); j++) {
					newBoundary.append(points.get(j).latitude);
					newBoundary.append(",");
					newBoundary.append(points.get(j).longitude);
					newBoundary.append(",");
				}
				newBoundary.deleteCharAt(newBoundary.length() - 1);
				buildNewPolygons.append(newBoundary.toString() + ";");
			}
		}
		if(buildNewPolygons.length() > 0){
			buildNewPolygons.deleteCharAt(buildNewPolygons.length() - 1);
		}
		this.strPolygons = buildNewPolygons.toString();
	}
	public void removePolygons(){
		for(int i=0; i<myPolygons.size(); i++){
			myPolygons.get(i).remove();
		}
	}
	public void removePolygon(MyPolygon poly){
		myPolygons.remove(poly);
	}
	public Integer getId() {
		return id;
	}
	public String getRemote_id() {
		return remote_id;
	}
	public Integer getHasChanged() {
		return hasChanged;
	}
	public String getDateChanged() {
		return dateChanged;
	}
	public String getFieldName() {
		return fieldName;
	}
	public String getComment() {
		return comment;
	}
	public String getStrPolygons() {
		return strPolygons;
	}
	public List<PolygonOptions> getPolygons() {
		//Convert strPolygons to polygons
		List<PolygonOptions> polygons = new ArrayList<PolygonOptions>();

		String allPolygons = this.getStrPolygons();
		if(allPolygons != null){
			StringTokenizer tokensBoundarys = new StringTokenizer(allPolygons, ";");
			while (tokensBoundarys.hasMoreTokens()) {
				PolygonOptions polygonOptions = new PolygonOptions();
				polygonOptions.fillColor(Field.FILL_COLOR_NOT_PLANNED);
				polygonOptions.strokeWidth(Field.STROKE_WIDTH);
				polygonOptions.strokeColor(Field.STROKE_COLOR);
				
				String boundary = tokensBoundarys.nextToken();
				StringTokenizer tokensPoints = new StringTokenizer(boundary, ",");
				while (tokensPoints.hasMoreTokens()) {
					String lat = tokensPoints.nextToken();
					String lng = tokensPoints.nextToken();
					polygonOptions.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
				}
				polygons.add(polygonOptions);
			}
		}
		return polygons;
	}
	public List<MyPolygon> getMyPolygons() {
		return this.myPolygons;
	}
	public Integer getColor() {
		return color;
	}
	public Integer getVisible() {
		return visible;
	}
	public Integer getDeleted() {
		return deleted;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public void setRemote_id(String remote_id) {
		this.remote_id = remote_id;
	}
	public void setHasChanged(Integer hasChanged) {
		this.hasChanged = hasChanged;
	}
	public void setDateChanged(String dateChanged) {
		this.dateChanged = dateChanged;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public void setStrPolygons(String strPolygons) {
		this.strPolygons = strPolygons;
	}
	public void setPolygons(List<PolygonOptions> polygons) {
		this.polygons = polygons;
	}
	public void setColor(Integer color) {
		this.color = color;
	}
	public void setVisible(Integer visible){
		this.visible = visible;
	}
	public void setDeleted(Integer deleted) {
		this.deleted = deleted;
	}
		
	public static Note cursorToNote(Cursor cursor){
		if(cursor != null){
			Note note = new Note();
			note.setId(cursor.getInt(cursor.getColumnIndex(TableNotes.COL_ID)));
			note.setRemote_id(cursor.getString(cursor.getColumnIndex(TableNotes.COL_REMOTE_ID)));
			note.setHasChanged(cursor.getInt(cursor.getColumnIndex(TableNotes.COL_HAS_CHANGED)));
			note.setDateChanged(cursor.getString(cursor.getColumnIndex(TableNotes.COL_DATE_CHANGED)));
			note.setFieldName(cursor.getString(cursor.getColumnIndex(TableNotes.COL_FIELD_NAME)));
			note.setComment(cursor.getString(cursor.getColumnIndex(TableNotes.COL_COMMENT)));
			note.setStrPolygons(cursor.getString(cursor.getColumnIndex(TableNotes.COL_POLYGONS)));
			//TODO lines, points
			note.setColor(cursor.getInt(cursor.getColumnIndex(TableNotes.COL_COLOR)));
			note.setVisible(cursor.getInt(cursor.getColumnIndex(TableNotes.COL_VISIBLE)));
			note.setDeleted(cursor.getInt(cursor.getColumnIndex(TableNotes.COL_DELETED)));
			return note;
		} else {
			return null;
		}
	}
	
	public static List<Note> FindNotesByFieldName(SQLiteDatabase database, String fieldName){
		if (fieldName != null) {
			// Find current field
			List<Note> notes = new ArrayList<Note>();
			String where = TableNotes.COL_FIELD_NAME + " = '" + fieldName + "' AND " + TableFields.COL_DELETED + " = 0";;
			Cursor cursor = database.query(TableNotes.TABLE_NAME,TableNotes.COLUMNS, where, null, null, null, null);
			while(cursor.moveToNext()) {
				notes.add(Note.cursorToNote(cursor));
			}
			cursor.close();
			return notes;
		} else {
			return null;
		}
	}
}
