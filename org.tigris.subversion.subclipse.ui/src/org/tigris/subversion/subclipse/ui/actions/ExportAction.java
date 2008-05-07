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
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.ExportOperation;

public class ExportAction extends WorkbenchWindowAction {
	private IDialogSettings settings;
	private final static String settingsKey = "ExportAction.lastLocation"; //$NON-NLS-1$

	public ExportAction() {
		super();
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		dialog.setText(Policy.bind("ExportAction.exportTo")); //$NON-NLS-1$
		dialog.setFilterPath(getLastLocation());
		String directory = dialog.open();
		if (directory == null) return;
		saveLocation(directory);
		new ExportOperation(getTargetPart(), getSelectedResources(), directory).run();
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_EXPORT;
	}
	
	private String getLastLocation() {
		String lastLocation = null;
		IResource[] resources = getSelectedResources();
		
		// If just one resource selected, first see if this resource has
		// previously been exported.
		if (resources.length == 1) {
			lastLocation = settings.get(settingsKey + "." + resources[0].getFullPath()); //$NON-NLS-1$
		}
		if (lastLocation == null) {
			lastLocation = settings.get(settingsKey);
		}
		return lastLocation;
	}
	
	private void saveLocation(String directory) {
		IResource[] resources = getSelectedResources();
		if (resources.length == 1) {
			settings.put(settingsKey + "." + resources[0].getFullPath(), directory); //$NON-NLS-1$
		}
		settings.put(settingsKey, directory);
	}

}
