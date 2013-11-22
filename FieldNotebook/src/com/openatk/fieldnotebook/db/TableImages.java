package com.openatk.fieldnotebook.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TableImages {
	// Database table
	public static final String TABLE_NAME = "images";
	public static final String COL_ID = "_id";
	public static final String COL_NOTE_ID = "note_id";
	public static final String COL_IMAGE = "image";
	public static final String COL_PATH = "path";

	public static String[] COLUMNS = { COL_ID, COL_NOTE_ID, COL_IMAGE, COL_PATH };
	
	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
	      + TABLE_NAME
	      + "(" 
	      + COL_ID + " integer primary key autoincrement," 
	      + COL_NOTE_ID + " text default ''," 
	      + COL_IMAGE + " text," 
	      + COL_PATH + " text"
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
