package com.openatk.fieldnotebook.imageviewer;

import com.openatk.fieldnotebook.db.Image;

/**
 * Interface to dispatch image selections.
 * 
 * @author Cyrus Bowman
 */
public interface ImageViewerListener {

	/**
	 * Inform the listener that an image has been selected.
	 * 
	 * @param imageItem
	 * @param position
	 */
	public void ImageViewerRequestData(FragmentImageViewer requester);
	public void ImageViewerDone(Image image);
}