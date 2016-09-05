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
package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.core.util.ISimpleDialogsHelper;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.eclipse.swt.graphics.Image;

/**
 *	
 *	This class is a dialog helper class for the core package.
 *  It's made availabe for core thru SVNProviderPlugin.getSimpleDialogsHelper().
 *	New simple dialogs should be added here so the minimum of glue is needed for
 *  every new dialog. Remember to update the ISimpleDialogsHelper interface. 
 * 
 * @author Magnus Naeslund (mag@kite.se)
 * @see org.tigris.subversion.subclipse.core.util.ISimpleDialogsHelper 
 * @see org.tigris.subversion.subclipse.core.SVNProviderPlugin#getSimpleDialogsHelper()
 */

public class SimpleDialogsHelper implements ISimpleDialogsHelper {

	public boolean promptYesNo(String title, String question, boolean yesIsDefault) {
		MessageDialogRunnable mdr = new MessageDialogRunnable(
				null,
                title,
                null,
                question,
                MessageDialog.QUESTION,
                new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
                yesIsDefault ? 0 : 1);
        SVNUIPlugin.getStandardDisplay().syncExec(mdr);
		return mdr.getResult() == 0;
	}

	public boolean promptYesCancel(String title, String question, boolean yesIsDefault) {
		MessageDialogRunnable mdr = new MessageDialogRunnable(
				null,
                title,
                null,
                question,
                MessageDialog.QUESTION,
                new String[] {IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL},
                yesIsDefault ? 0 : 1);
        SVNUIPlugin.getStandardDisplay().syncExec(mdr);
		return mdr.getResult() == 0;
	}
	
	/**
	 * 
	 * This should be reused for all MessageDialog type of dialogs.
	 * 
	 * @author mag
	 *
	 */
	
	private static class MessageDialogRunnable implements Runnable {
		final Shell shell;
		final String title, message;
		final Image image;
		final int imageType, defaultButton;
		final String buttonLabels[];
		int result;
		
		/**
		 * 
		 * @param shell if null, it's Display.getCurrent().getActiveShell()
		 * @param title
		 * @param image can be null
		 * @param message
		 * @param imageType
		 * @param buttonLabels
		 * @param defaultButton
		 */

		MessageDialogRunnable(Shell shell, String title, Image image, String message, int imageType, String buttonLabels[], int defaultButton){
			this.shell = shell;
			this.title = title;
			this.image = image;
			this.message = message;
			this.imageType = imageType;
			this.buttonLabels = buttonLabels;
			this.defaultButton = defaultButton;  
		}
		
		public void run() {
			result = new MessageDialog(
					shell != null ? shell : Display.getCurrent().getActiveShell(),
					title, image, message, imageType,
					buttonLabels, defaultButton).open();
		}
		
		public int getResult(){
			return result;
		}
	}

}
