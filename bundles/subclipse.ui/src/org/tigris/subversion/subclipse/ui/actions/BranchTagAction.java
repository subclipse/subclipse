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
import org.eclipse.jface.wizard.WizardDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.BranchTagOperation;
import org.tigris.subversion.subclipse.ui.wizards.BranchTagWizard;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagAction extends WorkbenchWindowAction {

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
	        IResource[] resources = getSelectedResources();
        	BranchTagWizard wizard = new BranchTagWizard(resources);
        	SizePersistedWizardDialog dialog = new SizePersistedWizardDialog(getShell(), wizard, "BranchTag"); //$NON-NLS-1$
        	wizard.setParentDialog(dialog);
        	if (dialog.open() == WizardDialog.OK) {	
        		SVNUrl[] sourceUrls = wizard.getUrls();
        		SVNUrl destinationUrl = wizard.getToUrl();
        		String message = wizard.getComment();
        		boolean createOnServer = wizard.isCreateOnServer();
	            BranchTagOperation branchTagOperation = new BranchTagOperation(getTargetPart(), getSelectedResources(), sourceUrls, destinationUrl, createOnServer, wizard.getRevision(), message);
	            branchTagOperation.setMakeParents(wizard.isMakeParents());
	            branchTagOperation.setMultipleTransactions(wizard.isSameStructure());
	            branchTagOperation.setNewAlias(wizard.getNewAlias());
	            branchTagOperation.switchAfterTagBranchOperation(wizard.isSwitchAfterBranchTag());
	            branchTagOperation.setSvnExternals(wizard.getSvnExternals());
	            branchTagOperation.run();        		
        	}
        }
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("BranchTagAction.branch"); //$NON-NLS-1$
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
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
    protected boolean isEnabledForAddedResources() {
        return false;
    }

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_BRANCHTAG;
	}
}
