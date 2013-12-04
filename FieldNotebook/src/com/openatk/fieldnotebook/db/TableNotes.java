package com.openatk.fieldnotebook.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TableNotes {
	// Database table
	public static final String TABLE_NAME = "notes";
	public static final String COL_ID = "_id";
	public static final String COL_REMOTE_ID = "remote_id";
	public static final String COL_HAS_CHANGED = "has_changed";
	public static final String COL_DATE_CHANGED = "date_changed";
	public static final String COL_FIELD_NAME = "field_name"; //TODO switch to fieldname
	public static final String COL_COMMENT = "comment";
	public static final String COL_POLYGONS = "polygons";
	public static final String COL_LINES = "lines";
	public static final String COL_POINTS = "points";
	public static final String COL_IMAGEPOINTS = "imagepoints";
	public static final String COL_COLOR = "color";
	public static final String COL_VISIBLE = "visible";
	public static final String COL_DELETED = "deleted";

	public static String[] COLUMNS = { COL_ID, COL_REMOTE_ID, COL_HAS_CHANGED, 
										COL_DATE_CHANGED, COL_FIELD_NAME, COL_COMMENT, COL_POLYGONS, 
										COL_LINES, COL_POINTS, COL_IMAGEPOINTS, COL_COLOR, COL_VISIBLE, COL_DELETED };
	
	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
	      + TABLE_NAME
	      + "(" 
	      + COL_ID + " integer primary key autoincrement," 
	      + COL_REMOTE_ID + " text default ''," 
	      + COL_HAS_CHANGED + " integer," 
	      + COL_DATE_CHANGED + " text,"
	      + COL_FIELD_NAME + " text,"
	      + COL_COMMENT + " text,"
	      + COL_POLYGONS + " text,"
	      + COL_LINES + " text,"
	      + COL_POINTS + " text,"
	      + COL_IMAGEPOINTS + " text,"
	      + COL_COLOR + " integer,"
	      + COL_VISIBLE + " integer default 1,"
	      + COL_DELETED + " integer default 0"
	      + ");";

	public static void onCreate(SQLiteDatabase database) {
	  database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.d("TableNotes - onUpgrade", "Upgrade from " + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));
    	int version = oldVersion;
    	switch(version){
    		case 1: //Launch
    			//Do nothing this is the gplay launch version
    		case 2: //V2
    			//Nothing changed in this table
    	}
	    //database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	    //onCreate(database);
	}
}
