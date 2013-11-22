package com.openatk.fieldnotebook.sidebar;

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
	public void SidebarRequestData(FragmentSidebar requester);
	public void SidebarEditField();
	public void SidebarBackToFieldsList();
	public void SidebarAddField();
}