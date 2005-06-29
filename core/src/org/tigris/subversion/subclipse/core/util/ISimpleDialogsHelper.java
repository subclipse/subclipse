
/**
 *
 * This interface exists to provide the UI package a way to pass dialogs 
 * helpers to the subclipse core package. 
 * 
 * @author Magnus Naeslund (mag@kite.se)
 * 
 */

package org.tigris.subversion.subclipse.core.util;

/**
 * 
 * @author mag
 * @see org.tigris.subversion.subclipse.ui.util.SimpleDialogsHelper
 * @see org.tigris.subversion.subclipse.core.SVNProviderPlugin#getSimpleDialogsHelper()
 *
 */

public interface ISimpleDialogsHelper {
	
	/**
	 * 
	 * @param title
	 * @param question
	 * @param yesIsDefault
	 * @return true if the user pressed yes
	 * 
	 */
	
	public boolean promptYesNo(String title, String question, boolean yesIsDefault);
	public boolean promptYesCancel(String title, String question, boolean yesIsDefault);
	
	
}
