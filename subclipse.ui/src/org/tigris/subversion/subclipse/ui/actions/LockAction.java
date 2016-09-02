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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.LockResourcesCommand;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardLockPage;

public class LockAction extends WorkbenchWindowAction {

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
        	if (getSelectedResources() != null && getSelectedResources().length > 0) {
		        final IResource[] resources = getSelectedResources();
		        SvnWizardLockPage lockPage = new SvnWizardLockPage(resources);
		        SvnWizard wizard = new SvnWizard(lockPage);
		        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
		        wizard.setParentDialog(dialog);
		        if (dialog.open() == SvnWizardDialog.OK) {
		            final String comment = lockPage.getComment();
		            final boolean stealLock = lockPage.isStealLock();
		            run(new WorkspaceModifyOperation() {
		                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		                    try {
		    					Hashtable table = getProviderMapping(getSelectedResources());
		    					Set keySet = table.keySet();
		    					Iterator iterator = keySet.iterator();
		    					while (iterator.hasNext()) {
		    					    SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
		    				    	LockResourcesCommand command = new LockResourcesCommand(provider.getSVNWorkspaceRoot(), resources, stealLock, comment);
		    				        command.run(Policy.subMonitorFor(monitor,1000));    					
		    					}
		                    } catch (TeamException e) {
		    					throw new InvocationTargetException(e);
		    				} finally {
		    					monitor.done();
		    				}
		                }              
		            }, true /* cancelable */, PROGRESS_DIALOG);
		        }
        	}
        }
    }
    /**
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNResource)
     */
    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
        try {
        	boolean enabled = super.isEnabledForSVNResource(svnResource);
        	if (enabled)
        		return !svnResource.getStatusFromCache().isLocked();
        	else
        		return enabled;
        } catch (SVNException e) {
            return false;
        }
    }
	protected boolean isEnabledForAddedResources() {
		return false;
	}
	protected boolean isEnabledForIgnoredResources() {
		return false;
	}
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_LOCK;
	}

}
