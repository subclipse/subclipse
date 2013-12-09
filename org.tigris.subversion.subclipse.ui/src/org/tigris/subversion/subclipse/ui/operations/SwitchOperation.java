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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.SwitchToUrlCommand;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.subclipse.ui.decorator.SVNLightweightDecorator;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SwitchOperation extends RepositoryProviderOperation {
    private SVNRevision svnRevision;
    private HashMap<IResource, SVNUrl> urlMap = new HashMap<IResource, SVNUrl>();
    
    private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
    private boolean setDepth = false;
    private boolean ignoreExternals = false;
    private boolean force = true; 
    private boolean ignoreAncestry = false;
    private boolean canRunAsJob = true;
    private ISVNConflictResolver conflictResolver;
    
    public SwitchOperation(IWorkbenchPart part, IResource[] resources, SVNUrl[] svnUrls, SVNRevision svnRevision) {
        super(part, resources);
        this.svnRevision = svnRevision;
        for (int i = 0; i < resources.length; i++) 
        	urlMap.put(resources[i], svnUrls[i]);
    }
    
    protected String getTaskName() {
        return Policy.bind("SwitchOperation.taskName"); //$NON-NLS-1$;
    }

    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("SwitchOperation.0", provider.getProject().getName()); //$NON-NLS-1$       
    }

    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask("Switch to Branch/Tag", resources.length);
		try {
			final List<IProject> projectList = new ArrayList<IProject>();
			for (int i = 0; i < resources.length; i++) {
				monitor.subTask("Switching " + resources[i].getName() + ". . .");
				SVNUrl svnUrl = (SVNUrl)urlMap.get(resources[i]);
				if (conflictResolver != null && conflictResolver instanceof SVNConflictResolver) {
					((SVNConflictResolver)conflictResolver).setPart(getPart());
				}
				SVNWorkspaceSubscriber.getInstance().updateRemote(resources);
		    	SwitchToUrlCommand command = new SwitchToUrlCommand(provider.getSVNWorkspaceRoot(),resources[i], svnUrl, svnRevision);
		        command.setDepth(depth);
		        command.setSetDepth(setDepth);
		        command.setIgnoreExternals(ignoreExternals);
		        command.setForce(force);
		        command.setIgnoreAncestry(ignoreAncestry);
		        command.setConflictResolver(conflictResolver);
		    	command.run(monitor);
		        monitor.worked(1);
		        if (resources[i].getProject() != null && !projectList.contains(resources[i].getProject())) {
		        	projectList.add(resources[i].getProject());
		        }
			}
			// Trigger lightweight refresh of decorators for project.  This is needed because refreshLocal is not triggering a refresh for unchanged
			// resources in Project Explorer.
			Display.getDefault().asyncExec(new Runnable() {				
				public void run() {
					SVNLightweightDecorator decorator = (SVNLightweightDecorator)SVNUIPlugin.getPlugin().getWorkbench().getDecoratorManager().getBaseLabelProvider(SVNUIPlugin.DECORATOR_ID);
					for (IProject project : projectList) {
						decorator.refresh(project);
					}
				}
			});
		} catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
		} catch (TeamException e) {
		    collectStatus(e.getStatus());
        } finally {
            monitor.done();
		}
    }
    
	protected boolean canRunAsJob() {
		return canRunAsJob;
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

	public void setIgnoreAncestry(boolean ignoreAncestry) {
		this.ignoreAncestry = ignoreAncestry;
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	} 
	
	public void setConflictResolver(ISVNConflictResolver conflictResolver) {
		this.conflictResolver = conflictResolver;
	}
	
	protected ISchedulingRule getSchedulingRule(SVNTeamProvider provider) {
		return null;
	}

}
