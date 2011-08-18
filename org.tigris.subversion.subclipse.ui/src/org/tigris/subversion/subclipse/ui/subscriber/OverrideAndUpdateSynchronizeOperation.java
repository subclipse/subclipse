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
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.RevertResourcesCommand;
import org.tigris.subversion.subclipse.core.commands.UpdateResourcesCommand;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class OverrideAndUpdateSynchronizeOperation extends SVNSynchronizeOperation {
	private IResource[] modifiedResources;
	private IResource[] resources;
	private boolean revertAndUpdate = true;
	private boolean prompted;
	private List<IStatus> errors = new ArrayList<IStatus>(); // of IStatus
	
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

	public OverrideAndUpdateSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] modifiedResources, IResource[] resources) {
		super(configuration, elements);
		this.modifiedResources = modifiedResources;
		this.resources = resources;
	}

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (!revertAndUpdate) return;
		if (!prompted) {
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					revertAndUpdate = MessageDialog.openQuestion(getShell(), Policy.bind("SyncAction.override.title"), Policy.bind("SyncAction.override.confirm")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			prompted = true;
		}
		if (!revertAndUpdate) return;
		IResource[] modifiedResourceArray = extractResources(modifiedResources, set);
		IResource[] resourceArray = extractResources(resources, set);
		if (modifiedResourceArray != null && modifiedResourceArray.length > 0) { 
			monitor.beginTask(null, 100);
			try {
				new RevertResourcesCommand(provider.getSVNWorkspaceRoot(),modifiedResourceArray).run(Policy.subMonitorFor(monitor,100));
			} catch (SVNException e) {
			    collectStatus(e.getStatus());
			} finally {
	            monitor.done();
			}
		}
		if (monitor.isCanceled()) {
			return;
		}
		SVNRevision revision = SVNRevision.HEAD;
		monitor.beginTask(null, 100);
		try {	
			IResource[] incomingResources = getIncoming(resourceArray);
		    SVNWorkspaceSubscriber.getInstance().updateRemote(incomingResources);
	    	UpdateResourcesCommand command = new UpdateResourcesCommand(provider.getSVNWorkspaceRoot(),incomingResources, revision);
	    	command.setConflictResolver(new SVNConflictResolver());
	    	command.run(Policy.subMonitorFor(monitor,100));
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} catch (TeamException e) {
		    collectStatus(e.getStatus());
        } finally {
            monitor.done();
		}
	}
	
	protected boolean canRunAsJob() {
		return true;
	}

	protected String getJobName() {
		return Policy.bind("SyncAction.override.title"); //$NON-NLS-1$
	}

	private IResource[] getIncoming(IResource[] resources) throws TeamException {
		List<IResource> incomingResources = new ArrayList<IResource>();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			SVNStatusSyncInfo info = (SVNStatusSyncInfo) SVNWorkspaceSubscriber.getInstance().getSyncInfo(resource);	
			if (info != null) {
				if (SyncInfo.getDirection(info.getKind()) == SyncInfo.INCOMING || SyncInfo.getDirection(info.getKind()) == SyncInfo.CONFLICTING)
					incomingResources.add(resource);
			}
		}
		IResource[] incomingArray = new IResource[incomingResources.size()];
		incomingResources.toArray(incomingArray);
		return incomingArray;
	}
	
	private void collectStatus(IStatus status)  {
		if (isLastError(status)) return;
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

}
