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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.svnproperties.SvnPropertiesView;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardSetPropertyPage;

/**
 * Set a new svn property on a given resource 
 */
public class SetSvnPropertyAction extends WorkbenchWindowAction {
	
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
        	if(getSelectedResources().length > 0) {
				run(new WorkspaceModifyOperation() {
					public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
						IResource resource = getSelectedResources()[0];
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						SvnWizardSetPropertyPage setPropertyPage = new SvnWizardSetPropertyPage(svnResource);
						SvnWizard wizard = new SvnWizard(setPropertyPage);
					    SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
					    wizard.setParentDialog(dialog);  
						if (dialog.open() != SvnWizardDialog.OK) return;
					
						try {
							if (setPropertyPage.getPropertyValue() != null) {
								svnResource.setSvnProperty(setPropertyPage.getPropertyName(), setPropertyPage.getPropertyValue(),setPropertyPage.getRecurse());
							} else {
								svnResource.setSvnProperty(setPropertyPage.getPropertyName(), setPropertyPage.getPropertyFile(),setPropertyPage.getRecurse());
							}
							SvnPropertiesView.refreshView();
						} catch (SVNException e) {
							throw new InvocationTargetException(e);
						}
					} 
				}, false /* cancelable */, PROGRESS_BUSYCURSOR);
        	}
        }
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("SetSvnPropertyAction.set"); //$NON-NLS-1$
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
		return false;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_PROPSET;
	}	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
}
