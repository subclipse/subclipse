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
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.UpdateResourcesCommand;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class UpdateSynchronizeOperation extends SVNSynchronizeOperation {
	private IResource[] resources;
	private boolean confirm;
	private boolean confirmNeeded;
	private int statusCount;
	private List errors;
	
	public UpdateSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (confirmNeeded) {
			final SyncInfoSet syncSet = getSyncInfoSet();
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					confirm = MessageDialog.openConfirm(getShell(), Policy.bind("SyncAction.updateAll"), Policy.bind("SyncAction.updateConfirm", Integer.toString(syncSet.getSyncInfos().length))); //$NON-NLS-1$ //$NON-NLS-1$				
				}			
			});
			if (!confirm) return;			
		}
		errors = new ArrayList();
		statusCount = 0;
		super.run(monitor);
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IResource[] resourceArray = trimResources(extractResources(resources, set));
		SVNRevision revision = null;
		final SyncInfo[] syncInfos = set.getSyncInfos();
		boolean containsDeletes = false;
		for (int i = 0; i < syncInfos.length; i++) {
			SVNStatusSyncInfo syncInfo = (SVNStatusSyncInfo)syncInfos[i];
			IResourceVariant remote = syncInfo.getRemote();
			if (remote != null && remote instanceof ISVNRemoteResource) {
				if (syncInfo.getRemoteResourceStatus() != null && syncInfo.getRemoteResourceStatus().getTextStatus() == SVNStatusKind.DELETED) {
					containsDeletes = true;
					continue;
				} 
				SVNRevision rev = ((ISVNRemoteResource)remote).getLastChangedRevision();
				if (rev instanceof SVNRevision.Number) {
					long nbr = ((SVNRevision.Number)rev).getNumber();
					if (revision == null) revision = rev;
					else {
						long revisionNumber = ((SVNRevision.Number)revision).getNumber();
						if (nbr > revisionNumber) revision = rev;
					}
				}
			}
		}
		if (revision == null || containsDeletes) revision = SVNRevision.HEAD;
		
//		new UpdateOperation(getPart(), resourceArray, revision, true).run();
		try {	
			SVNWorkspaceSubscriber.getInstance().updateRemote(resourceArray);
	    	UpdateResourcesCommand command = new UpdateResourcesCommand(provider.getSVNWorkspaceRoot(),resourceArray, revision, true);
	        command.run(Policy.subMonitorFor(monitor,100));		
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} catch (TeamException e) {
		    collectStatus(e.getStatus());
        } finally {
            monitor.done();
		}
	}
	
	/**
	 * This method takes the array of resources to be updated and removes any
	 * items that have a parent folder that is also being updated, since the
	 * recursive update of a parent folder will cause the resource to be updated
	 * anyway.  This will make the update run faster.
	 * @param resourceArray
	 * @return
	 */
    private IResource[] trimResources(IResource[] resourceArray) {
    	// Get a list of just the folders.
        List folders = new ArrayList();
        for (int i = 0; i < resourceArray.length; i++) {
            if (resourceArray[i].getType() == IResource.FOLDER || resourceArray[i].getType() == IResource.PROJECT) 
                folders.add(resourceArray[i]);
        }
        
        List trimmedList = new ArrayList();
        for (int i = 0; i < resourceArray.length; i++) {
            if (!parentIncluded(resourceArray[i], folders))
                trimmedList.add(resourceArray[i]);
        }
        
        IResource[] trimmedArray = new IResource[trimmedList.size()];
		trimmedList.toArray(trimmedArray);
		return trimmedArray;
    }
    
    private boolean parentIncluded(IResource resource, List folders) {
        IResource parent = resource.getParent();
        if (parent == null) return false;
        if (folders.contains(parent)) return true;
        return parentIncluded(parent, folders);
    }

	public void setConfirmNeeded(boolean confirmNeeded) {
		this.confirmNeeded = confirmNeeded;
	}
	
	private void collectStatus(IStatus status)  {
		if (isLastError(status)) return;
		statusCount++;
		if (!status.isOK()) addError(status);
	}
	
	private boolean isLastError(IStatus status) {
		return (errors.size() > 0 && getLastError() == status);
	}
	
	private void addError(IStatus status) {
		if (status.isOK()) return;
		if (isLastError(status)) return;
		errors.add(status);
	}
	
	private IStatus getLastError() {
		Assert.isTrue(errors.size() > 0);
		IStatus status = (IStatus)errors.get(errors.size() - 1);
		return status;
	}
	
	protected boolean canRunAsJob() {
		return true;
	}
	
	protected String getJobName() {
		return Policy.bind("UpdateOperation.taskName"); //$NON-NLS-1$;
	}	
}
