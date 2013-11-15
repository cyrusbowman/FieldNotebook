package com.openatk.fieldnotebook.fieldlist;

import com.openatk.fieldnotebook.db.Field;

/**
 * Interface to dispatch image selections.
 * 
 * @author Cyrus Bowman
 */
public interface FieldListListener {

	/**
	 * Inform the listener that an image has been selected.
	 * 
	 * @param imageItem
	 * @param position
	 */

	public void FieldListRequestData(FragmentFieldList requester);
	public void FieldListAddNote();
	public void FieldListSelectField(Field selectedField);
}