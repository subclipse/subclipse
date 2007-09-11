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
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.dialogs.DifferencesDialog;
import org.tigris.subversion.subclipse.ui.dialogs.ShowDifferencesAsUnifiedDiffDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class ShowDifferencesAsUnifiedDiffAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		String fromRevision = null;
		String toRevision = null;
		ISVNResource[] selectedResources = getSelectedRemoteResources();
		if (selectedResources == null || !(selectedResources.length == 2)) {
			Object[] selectedObjects = selection.toArray();
			if (selectedObjects[0] instanceof ILogEntry && selectedObjects[1] instanceof ILogEntry) {
				selectedResources = new ISVNResource[2];
				selectedResources[0] = ((ILogEntry)selectedObjects[0]).getResource();
				selectedResources[1] = ((ILogEntry)selectedObjects[1]).getResource();
				fromRevision = ((ILogEntry)selectedObjects[0]).getRevision().toString();
				toRevision = ((ILogEntry)selectedObjects[1]).getRevision().toString();
			}
		} else {
			if (selectedResources[0] instanceof ISVNRemoteResource)
				fromRevision = ((ISVNRemoteResource)selectedResources[0]).getRevision().toString();
			if (selectedResources[1] instanceof ISVNRemoteResource)
				toRevision = ((ISVNRemoteResource)selectedResources[1]).getRevision().toString();			
		}
//		ShowDifferencesAsUnifiedDiffDialog dialog = new ShowDifferencesAsUnifiedDiffDialog(getShell(), selectedResources, getTargetPart());
		DifferencesDialog dialog = new DifferencesDialog(getShell(), null, selectedResources, getTargetPart());
		if (!fromRevision.equals("HEAD")) dialog.setFromRevision(fromRevision); //$NON-NLS-1$
		if (!toRevision.equals("HEAD")) dialog.setToRevision(toRevision); //$NON-NLS-1$  
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		Object[] selectedObjects = selection.toArray();
		if (selectedObjects.length != 2) return false;
		ISVNResource svnResource1 = null;
		ISVNResource svnResource2 = null;
		if (selectedObjects[0] instanceof ISVNResource) svnResource1 = (ISVNResource)selectedObjects[0];
		else {
			if (selectedObjects[0] instanceof ILogEntry)
				svnResource1 = ((ILogEntry)selectedObjects[0]).getResource();
		}
		if (selectedObjects[1] instanceof ISVNResource) svnResource2 = (ISVNResource)selectedObjects[1];
		else {
			if (selectedObjects[1] instanceof ILogEntry)
				svnResource2 = ((ILogEntry)selectedObjects[1]).getResource();		
		}
		if (svnResource1 == null || svnResource2 == null) return false;
		
		if (!svnResource1.getRepository().getRepositoryRoot().toString().equals(svnResource2.getRepository().getRepositoryRoot().toString())) return false;
		return (svnResource1.isFolder() == svnResource2.isFolder());
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_DIFF;
	}

}
