package com.openatk.fieldnotebook.notelist;

import com.google.android.gms.maps.model.PolygonOptions;
import com.openatk.fieldnotebook.FragmentDrawing;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.drawing.MyPolygon;

/**
 * Interface to dispatch image selections.
 * 
 * @author Cyrus Bowman
 */
public interface NoteListListener {

	/**
	 * Inform the listener that an image has been selected.
	 * 
	 * @param imageItem
	 * @param position
	 */
	//public void SliderEditField();

	public void NoteListRequestData(FragmentNoteList requester);
	public void NoteListCompletePolygon();
	public void NoteListAddPolygon();
	public void NoteListEditPolygon(MyPolygon poly);
	public FragmentDrawing NoteListShowDrawing();
	public void NoteListHideDrawing();
	public void NoteListAddNote();
	public void NoteListUpdatePolygon(int fieldId, PolygonOptions polygonOptions);
}