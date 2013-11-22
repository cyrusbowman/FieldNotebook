package com.openatk.fieldnotebook.db;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;



public class Image {
	private Integer id = null;
	private Integer note_id = null;

	private Bitmap thumb = null;
	private String path = null;

	public Image() {
		
	}

	public Image(Bitmap thumb, String path) {
		this.path = path;
		this.thumb = thumb;
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public Integer getNoteId() {
		return this.note_id;
	}

	public void setNoteId(Integer note_id) {
		this.note_id = note_id;
	}

	public void setThumb(Bitmap thumb) {
		this.thumb = thumb;
	}

	public Bitmap getThumb() {
		return this.thumb;
	}
	
	public static Image cursorToImage(Cursor cursor){
		if(cursor != null){
			Image it = new Image();
			it.setId(cursor.getInt(cursor.getColumnIndex(TableImages.COL_ID)));
			it.setNoteId(cursor.getInt(cursor.getColumnIndex(TableImages.COL_NOTE_ID)));
			it.setPath(cursor.getString(cursor.getColumnIndex(TableImages.COL_PATH)));
			byte[] imageByteArray = cursor.getBlob(cursor.getColumnIndex(TableImages.COL_IMAGE));
			Bitmap thumb = null;
			try{
				ByteArrayInputStream imageStream = new ByteArrayInputStream(imageByteArray);
				thumb = BitmapFactory.decodeStream(imageStream);
	        } catch(Exception e){
	            Log.v("cursorToImage", e.getMessage());
	        }
			it.setThumb(thumb);
			return it;
		} else {
			return null;
		}
	}
		
	public static Image FindImageById(SQLiteDatabase database, Integer imageId){
		if(imageId != null){
			Image theImage = null;
			String where = TableImages.COL_ID + " = " + Integer.toString(imageId);
			Cursor cursor = database.query(TableFields.TABLE_NAME, TableFields.COLUMNS, where, null, null, null, null);
			if(cursor.moveToFirst()) {
				theImage = Image.cursorToImage(cursor);
			}
			cursor.close();
			return theImage;
		} else {
			return null;
		}
	}

	public static List<Image> FindImagesByNoteId(SQLiteDatabase database, Integer noteId) {
		// Find current field
		List<Image> images = new ArrayList<Image>();
		String where = TableImages.COL_NOTE_ID + " = " + Integer.toString(noteId);
		Cursor cursor = database.query(TableImages.TABLE_NAME, TableImages.COLUMNS, where, null, null, null, null);
		while(cursor.moveToNext()) {
			images.add(Image.cursorToImage(cursor));
		}
		cursor.close();
		return images;
	}
}
