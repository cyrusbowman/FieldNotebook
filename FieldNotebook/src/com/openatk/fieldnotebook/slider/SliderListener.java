package com.openatk.fieldnotebook.slider;

/**
 * Interface to dispatch image selections.
 * 
 * @author Cyrus Bowman
 */
public interface SliderListener {

	/**
	 * Inform the listener that an image has been selected.
	 * 
	 * @param imageItem
	 * @param position
	 */
	public void SliderAddNote();
	public void SliderRequestData(FragmentSlider requester);
	public void SliderEditField();
}