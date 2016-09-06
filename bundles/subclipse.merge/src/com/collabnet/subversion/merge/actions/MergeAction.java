/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.actions.WorkbenchWindowAction;

import com.collabnet.subversion.merge.wizards.CustomWizardDialog;
import com.collabnet.subversion.merge.wizards.MergeWizard;

// public class MergeAction extends WorkspaceAction {
public class MergeAction extends WorkbenchWindowAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        }  else {		
			IResource[] resources = getSelectedResources();
			MergeWizard wizard = new MergeWizard(resources, getTargetPart());
			WizardDialog dialog = new CustomWizardDialog(shell, wizard);
			dialog.open();	
        }
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForManagedResources()
	 */
	protected boolean isEnabledForManagedResources() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		try {
			// Must all be from same repository.
			ISVNRepositoryLocation repository = null;
			IResource[] selectedResources = getSelectedResources();
			for (int i = 0; i < selectedResources.length; i++) {
				ISVNRepositoryLocation compareToRepository = null;
				ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(selectedResources[i]);
				if (svnResource == null || !svnResource.isManaged()) {
					return false;
				}
				LocalResourceStatus status = svnResource.getStatusFromCache();
				if (status != null) {
					compareToRepository = status.getRepository();
				}
				if (compareToRepository == null) {
					return false;
				}
				if (repository != null && !compareToRepository.equals(repository)) {
					return false;
				}
				repository = compareToRepository;
			}
			return true;
		} catch (Exception e) { return false; }
	}	   	        
	
	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_MERGE;
	}

}
