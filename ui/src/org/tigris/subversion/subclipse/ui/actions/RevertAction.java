/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;
 
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.RevertDialog;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

/**
 * Action to restore pristine working copy file 
 */
public class RevertAction extends WorkspaceAction {
    
    private String url;
	private IResource[] resourcesToRevert;
    
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
		final IResource[] resources = getSelectedResources();
        try {
            IResource[] modifiedResources = getModifiedResources(resources, new NullProgressMonitor());
            if (!confirmRevert(modifiedResources)) return;
            new RevertOperation(getTargetPart(), resourcesToRevert).run();
        } catch (SVNException e) {
            throw new InvocationTargetException(e);
        }
	}
	
	/**
	 * get the modified resources in resources parameter
	 */	
	private IResource[] getModifiedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
	    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    final List modified = new ArrayList();
		final SVNException[] exception = new SVNException[] { null };		
	    for (int i = 0; i < resources.length; i++) {
			 IResource resource = resources[i];
			 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			 
			 // if only one resource selected, get url.  Revert dialog displays this.
			 if (resources.length == 1) {
				   url = svnResource.getStatus().getUrlString();
				   if ((url == null) || (resource.getType() == IResource.FILE)) url = Util.getParentUrl(svnResource);
			 }
			 
			 // get adds, deletes, updates and property updates.
			 GetStatusCommand command = new GetStatusCommand(svnResource, true, false);
			 command.run(iProgressMonitor);
			 LocalResourceStatus[] statuses = command.getStatuses();
			 for (int j = 0; j < statuses.length; j++) {
			     if (statuses[j].isTextModified() || statuses[j].isAdded() || statuses[j].isDeleted() || statuses[j].isMissing() || statuses[j].isReplaced() || statuses[j].getPropStatus().equals(SVNStatusKind.MODIFIED) || statuses[j].isTextConflicted() || statuses[j].isPropConflicted()) {
			         IResource currentResource = null;
			         currentResource = statuses[j].getResource();
			         if (currentResource != null)
			             modified.add(currentResource);
			     }
			 }
		}
	    return (IResource[]) modified.toArray(new IResource[modified.size()]);
	}
	
	/**
	 * prompt revert of selected resources.
	 */		
	protected boolean confirmRevert(IResource[] modifiedResources) {
	   if (modifiedResources.length == 0) return false;
	   RevertDialog dialog = new RevertDialog(getShell(), modifiedResources, url);
	   boolean revert = (dialog.open() == RevertDialog.OK);
	   url = null;
	   resourcesToRevert = dialog.getSelectedResources();
	   return revert;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("RevertAction.error"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForManagedResources()
	 */
	protected boolean isEnabledForManagedResources() {
		return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}

    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForInaccessibleResources()
     */
    protected boolean isEnabledForInaccessibleResources() {
        return true;
    }
	
}
