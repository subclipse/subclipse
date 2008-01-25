/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
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
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardUpdatePage;

/**
 * UpdateAction performs a 'svn update' command on the selected resources.
 * If conflicts are present (file has been changed both remotely and locally),
 * the changes will be merged into the local file such that the user must
 * resolve the conflicts. 
 */
public class UpdateAction extends WorkbenchWindowAction {
	private IResource[] selectedResources;
	private boolean canRunAsJob = true;
	
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
	        IResource[] resources = getSelectedResources(); 
	        SvnWizardUpdatePage updatePage = new SvnWizardUpdatePage(resources);
	        SvnWizard wizard = new SvnWizard(updatePage);
	        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
	        wizard.setParentDialog(dialog);       
	        if (dialog.open() == SvnWizardDialog.OK) {
	        	UpdateOperation updateOperation = new UpdateOperation(getTargetPart(), resources, updatePage.getRevision());
		    	updateOperation.setDepth(updatePage.getDepth());
		    	updateOperation.setSetDepth(updatePage.isSetDepth());
		    	updateOperation.setForce(updatePage.isForce());
		    	updateOperation.setIgnoreExternals(updatePage.isIgnoreExternals());
		    	updateOperation.setCanRunAsJob(canRunAsJob);
	        	updateOperation.run();
	        }
        } 		
	}

	protected IResource[] getSelectedResources() {
		if (selectedResources == null) return super.getSelectedResources();
		else return selectedResources;
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("UpdateAction.updateerror"); //$NON-NLS-1$
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_UPDATE;
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	}

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}
	
}
