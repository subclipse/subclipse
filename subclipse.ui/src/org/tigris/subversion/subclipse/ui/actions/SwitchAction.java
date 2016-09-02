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
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.SwitchOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardSwitchPage;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Action to switch to branch/tag 
 */
public class SwitchAction extends WorkbenchWindowAction {
	private IResource[] selectedResources;
	private boolean canRunAsJob = true;

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
	        IResource[] resources = getSelectedResources();
	        
	        // Use different wizard page name if multiple resources selected so that
	        // page size and location will be saved and restored separately for
	        // single selection switch and multiple selection switch.
	        String pageName;
	        if (resources.length > 1) pageName = "SwitchDialogWithConflictHandling.multiple";
	        else pageName = "SwitchDialogWithConflictHandling"; //$NON-NLS-1$
	        
	        SvnWizardSwitchPage switchPage = new SvnWizardSwitchPage(pageName, resources);
	        SvnWizard wizard = new SvnWizard(switchPage);
	        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
	        wizard.setParentDialog(dialog);
	        if (dialog.open() == SvnWizardDialog.OK) {
	            SVNUrl[] svnUrls = switchPage.getUrls();
	            SVNRevision svnRevision = switchPage.getRevision();
	            SwitchOperation switchOperation = new SwitchOperation(getTargetPart(), resources, svnUrls, svnRevision);
	            switchOperation.setDepth(switchPage.getDepth());
	            switchOperation.setSetDepth(switchPage.isSetDepth());
	            switchOperation.setIgnoreExternals(switchPage.isIgnoreExternals());
	            switchOperation.setForce(switchPage.isForce());
	            switchOperation.setIgnoreAncestry(switchPage.isIgnoreAncestry());
	            switchOperation.setCanRunAsJob(canRunAsJob);
	            switchOperation.setConflictResolver(switchPage.getConflictResolver());
	            switchOperation.run();	        
	        }
        }
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("SwitchAction.switch"); //$NON-NLS-1$
	}

	protected IResource[] getSelectedResources() {
		if (selectedResources != null) return selectedResources;
		else return super.getSelectedResources();
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

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_SWITCH;
	}

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	}

}
