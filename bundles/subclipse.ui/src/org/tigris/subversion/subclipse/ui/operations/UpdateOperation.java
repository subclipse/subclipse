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
package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.UpdateResourcesCommand;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Panagiotis K
 */
public class UpdateOperation extends RepositoryProviderOperation {
	private final SVNRevision revision;
    private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
    private boolean setDepth = false;
    private boolean ignoreExternals = false;
    private boolean force = true;
    private boolean canRunAsJob = true;
    private ISVNConflictResolver conflictResolver;

    public UpdateOperation(IWorkbenchPart part, IResource resource, SVNRevision revision) {
        super(part, new IResource[] {resource});
        this.revision = revision;
    }
    
    public UpdateOperation(IWorkbenchPart part, IResource[] resources, SVNRevision revision) {
        super(part, resources);
        this.revision = revision;
    }    

    /* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return Policy.bind("UpdateOperation.taskName"); //$NON-NLS-1$;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(SVNTeamProvider provider) {
		return Policy.bind("UpdateOperation.0", provider.getProject().getName()); //$NON-NLS-1$
	}


    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.RepositoryProviderOperation#execute(org.tigris.subversion.subclipse.core.SVNTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
		try {		
			if (conflictResolver != null && conflictResolver instanceof SVNConflictResolver) {
				((SVNConflictResolver)conflictResolver).setPart(getPart());
			}
		    SVNWorkspaceSubscriber.getInstance().updateRemote(resources);
	    	UpdateResourcesCommand command = new UpdateResourcesCommand(provider.getSVNWorkspaceRoot(),resources, revision);
	        command.setDepth(depth);
	        command.setSetDepth(setDepth);
	        command.setIgnoreExternals(ignoreExternals);
	        command.setForce(force);
	        command.setConflictResolver(conflictResolver);
	    	command.run(Policy.subMonitorFor(monitor,100));
			//updateWorkspaceSubscriber(provider, resources, Policy.subMonitorFor(monitor, 5));
		} catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
		} catch (TeamException e) {
		    collectStatus(e.getStatus());
        } finally {
        	SVNProviderPlugin.broadcastSyncInfoChanges(resources, false);
            monitor.done();
		}
    }
    
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public void setSetDepth(boolean setDepth) {
		this.setDepth = setDepth;
	}    

	public void setIgnoreExternals(boolean ignoreExternals) {
		this.ignoreExternals = ignoreExternals;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	}

	protected boolean canRunAsJob() {
		return canRunAsJob;
	}

	public void setConflictResolver(ISVNConflictResolver conflictResolver) {
		this.conflictResolver = conflictResolver;
	}

	protected ISchedulingRule getSchedulingRule(SVNTeamProvider provider) {
		ISchedulingRule schedulingRule = super.getSchedulingRule(provider);
		if (schedulingRule != null && conflictResolver != null) {
			((SVNConflictResolver)conflictResolver).setSchedulingRule(schedulingRule);
		}
		return schedulingRule;
	}

}
