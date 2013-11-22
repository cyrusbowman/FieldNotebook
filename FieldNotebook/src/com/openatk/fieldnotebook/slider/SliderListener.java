package com.openatk.fieldnotebook.slider;

import com.openatk.fieldnotebook.sidebar.FragmentSidebar;

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
	public void SliderRequestData(FragmentSlider fragmentSlider);
	public void SliderEditField();
	public void SliderBackToFieldsList();
	public void SliderAddField();
}