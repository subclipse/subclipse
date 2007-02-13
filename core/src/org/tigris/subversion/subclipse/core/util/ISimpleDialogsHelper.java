/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/

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
