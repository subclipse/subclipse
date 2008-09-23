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
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.operations.ShowAnnotationOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardAnnotatePage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;

public class ShowAnnotationAction extends WorkbenchWindowAction {

	/**
	 * Action to open a SVN Annotate View
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		// try to enable action if not enabled, for keyboard activated actions
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
        	// Get the selected resource.
			final ISVNRemoteFile svnResource = getSingleSelectedSVNRemoteFile();
			execute(svnResource);
        }
	}

	public void execute(final ISVNRemoteFile svnResource) throws InvocationTargetException, InterruptedException {

		if (svnResource == null) {
			return;
		}
		
		SvnWizardAnnotatePage annotatePage = new SvnWizardAnnotatePage(svnResource);
		SvnWizard wizard = new SvnWizard(annotatePage);
        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
        wizard.setParentDialog(dialog);
        if (dialog.open() == SvnWizardDialog.CANCEL) return;

        ShowAnnotationOperation showAnnotationOperation = new ShowAnnotationOperation(getTargetPart(), svnResource, annotatePage.getFromRevision(), annotatePage.getToRevision(), annotatePage.isIncludeMergedRevisions(), annotatePage.isIgnoreMimeType());
        showAnnotationOperation.run();

	}
	
	/**
	 * Ony enabled for single resource selection
	 */
	protected boolean isEnabled() throws TeamException {
		ISVNRemoteFile resource = getSingleSelectedSVNRemoteFile();
		return (resource != null);
	}

	/**
	 * This action is called from one of a Resource Navigator a SVN Resource
	 * Navigator or a History Log Viewer. Return the selected resource as an
	 * ISVNRemoteFile
	 * 
	 * @return ICVSResource
	 * @throws SVNException
	 */
	protected ISVNRemoteFile getSingleSelectedSVNRemoteFile() {
		// Selected from a SVN Resource Navigator or History
		if (this.getSelectedRemoteFiles().length > 0) {
			ISVNRemoteFile[] svnResources = this.getSelectedRemoteFiles();
			if (svnResources.length == 1) {
				return svnResources[0];
			}
		}

		// Selected from a Resource Navigator
		IResource[] resources = getSelectedResources();
		if (resources.length == 1 && resources[0].getType() == IResource.FILE ) {
			try {
				return (ISVNRemoteFile)SVNWorkspaceRoot.getBaseResourceFor(resources[0]);
			} catch (SVNException e) {
				return null;
			}
		}
		return null;
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_ANNOTATE;
	}
}
