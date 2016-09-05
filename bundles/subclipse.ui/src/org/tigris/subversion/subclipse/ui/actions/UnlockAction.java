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
import org.tigris.subversion.subclipse.core.commands.UnlockResourcesCommand;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;

public class UnlockAction extends WorkbenchWindowAction {

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
        	if (getSelectedResources() != null && getSelectedResources().length > 0) {
		        final IResource[] resources = getSelectedResources(); 
		        run(new WorkspaceModifyOperation() {
		            protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		                try {
							Hashtable table = getProviderMapping(getSelectedResources());
							Set keySet = table.keySet();
							Iterator iterator = keySet.iterator();
							while (iterator.hasNext()) {
							    SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
						    	UnlockResourcesCommand command = new UnlockResourcesCommand(provider.getSVNWorkspaceRoot(), resources, false);
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

    /**
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNResource)
     */
    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
        try {
        	if (!super.isEnabledForSVNResource(svnResource)) {
        		return false;
        	}
            return svnResource.getStatusFromCache().isLocked();
        } catch (SVNException e) {
            return false;
        }
    }

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_LOCK;
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

}
