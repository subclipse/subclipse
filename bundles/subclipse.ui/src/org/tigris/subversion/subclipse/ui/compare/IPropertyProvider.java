package org.tigris.subversion.subclipse.ui.compare;

public interface IPropertyProvider {
	
	public void getProperties(boolean recursive);
	
	public String getLabel();
	
	public boolean isEditable();

}
