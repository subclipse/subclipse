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

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.dialogs.ExportRemoteFolderDialog;

public class ExportRemoteFolderAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ISVNRemoteResource[] resources = getSelectedRemoteResources();
		ExportRemoteFolderDialog dialog = new ExportRemoteFolderDialog(getShell(), resources[0], getTargetPart());
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteResources().length == 1;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_EXPORT;
	}

}
