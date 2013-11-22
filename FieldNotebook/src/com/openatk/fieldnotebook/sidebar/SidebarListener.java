package com.openatk.fieldnotebook.sidebar;

import com.openatk.fieldnotebook.sidebar.FragmentSidebar;

/**
 * Interface to dispatch image selections.
 * 
 * @author Cyrus Bowman
 */
public interface SidebarListener {

	/**
	 * Inform the listener that an image has been selected.
	 * 
	 * @param imageItem
	 * @param position
	 */
	public void SidebarAddNote();
	public void SidebarRequestData(FragmentSidebar fragmentSlider);
	public void SidebarEditField();
	public void SidebarBackToFieldsList();
	public void SidebarAddField();
	public void SliderAddNote();
	public void SliderRequestData(FragmentSidebar requester);
	public void SliderEditField();
}