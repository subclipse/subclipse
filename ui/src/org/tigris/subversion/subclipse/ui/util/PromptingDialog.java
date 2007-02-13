/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * A confirmation dialog helper that will either show a 'yes/no/yes to all/cancel'
 * dialog to confirm an action performed on several resources or if only one
 * resource is specified 'ok/cancel' will be shown.
 */
public class PromptingDialog {
	private IResource[] resources;
	private Shell shell;
	private String[] buttons;
	private boolean confirmOverwrite = true;
	private IPromptCondition condition;
	private String title;
	private boolean hasMultipleResources;

	/**
	 * Prompt for the given resources using the specific condition. The prompt dialog will
	 * have the title specified.
	 */
	public PromptingDialog(Shell shell, IResource[] resources, IPromptCondition condition, String title) {
		this.condition = condition;
		this.resources = resources;
		this.title = title;
		this.shell = shell;
		this.hasMultipleResources = resources.length > 1;
		if (hasMultipleResources) {
			buttons = new String[] {
				IDialogConstants.YES_LABEL, 
				IDialogConstants.YES_TO_ALL_LABEL, 
				IDialogConstants.NO_LABEL, 
				IDialogConstants.CANCEL_LABEL};
		} else {
			buttons = new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL};
		}			 
	}
		
	/**
	 * Call to calculate and show prompt. If no resources satisfy the prompt condition
	 * a dialog won't be shown. The resources for which the user confirmed the action
	 * are returned.
	 */
	public IResource[] promptForMultiple() throws InterruptedException {
		List targetResources = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (condition.needsPrompt(resource) && confirmOverwrite) {
				if (confirmOverwrite(condition.promptMessage(resource))) {
					targetResources.add(resource);
				}
			} else {
				targetResources.add(resource);
			}						
		}
		return (IResource[]) targetResources.toArray(new IResource[targetResources.size()]);
	}
	
	/**
	 * Opens the confirmation dialog based on the prompt condition settings.
	 */
	private boolean confirmOverwrite(String msg) throws InterruptedException { 
		if (!confirmOverwrite) {
			return true;
		}
		final MessageDialog dialog = 
			new MessageDialog(shell, title, null, msg, MessageDialog.QUESTION, buttons, 0);

		// run in syncExec because callback is from an operation,
		// which is probably not running in the UI thread.
		shell.getDisplay().syncExec(
			new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		if (hasMultipleResources) {
			switch (dialog.getReturnCode()) {
				case 0://Yes
					return true;
				case 1://Yes to all
					confirmOverwrite = false; 
					return true;
				case 2://No
					return false;
				case 3://Cancel
				default:
					throw new InterruptedException();
			}
		} else {
			return dialog.getReturnCode() == 0;
		}
	}
}