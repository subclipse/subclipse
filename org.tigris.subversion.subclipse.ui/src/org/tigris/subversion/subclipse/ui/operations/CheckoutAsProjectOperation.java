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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CheckoutAsProjectOperation extends SVNOperation {
    private ISVNRemoteFolder[] remoteFolders;
    private IProject[] localFolders;
    private IPath projectRoot;
    private SVNRevision svnRevision = SVNRevision.HEAD;
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean ignoreExternals = false;
    private boolean force = true;

    public CheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders) {
    	this(part, remoteFolders, localFolders, null);
    }
    
    public CheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders, IPath projectRoot) {
        super(part);
        this.remoteFolders = remoteFolders;
        this.localFolders = localFolders;
        this.projectRoot = projectRoot; 
    }
    
    protected String getTaskName() {
        return Policy.bind("CheckoutAsProjectOperation.taskName"); //$NON-NLS-1$;
    }

    public void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
    	// First checkout all projects, then bring them into workspace.
    	List failedProjects = new ArrayList();
        monitor.beginTask(null, remoteFolders.length * 1000);
        for (int i = 0; i < remoteFolders.length; i++) {
            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
            ISchedulingRule rule = localFolders[i].getWorkspace().getRuleFactory().modifyRule(localFolders[i]);
			try {
				Job.getJobManager().beginRule(rule, monitor);
				monitor.setTaskName(Policy.bind("CheckoutAsProjectOperation.0", remoteFolders[i].getName())); //$NON-NLS-1$
				IProject[] local = new IProject[1];
				local[0] = localFolders[i];
				ISVNRemoteFolder[] remote = new ISVNRemoteFolder[1];
				remote[0] = remoteFolders[i];
				if (!execute(remote, local, subMonitor)) {
					failedProjects.add(remoteFolders[i]);
				}
			} finally {
				Job.getJobManager().endRule(rule);
			}            
        }
        for (int i = 0; i < remoteFolders.length; i++) {
        	if (!failedProjects.contains(remoteFolders[i])) {
	        	IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
	        	ISchedulingRule rule = localFolders[i].getWorkspace().getRuleFactory().modifyRule(localFolders[i]);
				try {
					Job.getJobManager().beginRule(rule, monitor);
					monitor.setTaskName(Policy.bind("SVNProvider.Creating_project_1", remoteFolders[i].getName())); //$NON-NLS-1$
					refreshProject(localFolders[i], subMonitor);
				} finally {
					Job.getJobManager().endRule(rule);
				}   
        	}
        }
    }
    
    protected boolean execute(ISVNRemoteFolder[] remote, IProject[] local, IProgressMonitor monitor) throws SVNException, InterruptedException {
    	try {
			CheckoutCommand command;
			if (projectRoot==null) {
				command = new CheckoutCommand(remote, local);
			} else {
				command = new CheckoutCommand(remote, local, projectRoot);
			}
			command.setSvnRevision(svnRevision);
			command.setDepth(depth);
			command.setIgnoreExternals(ignoreExternals);
			command.setForce(force);
			command.setRefreshProjects(false);
	    	command.run(monitor);
		} catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
			return false;
		}
		return true;
    }

	public void setSvnRevision(SVNRevision svnRevision) {
		this.svnRevision = svnRevision;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setIgnoreExternals(boolean ignoreExternals) {
		this.ignoreExternals = ignoreExternals;
	}

	public void setForce(boolean force) {
		this.force = force;
	}		
	
	/*
	 * Bring the provided projects into the workspace
	 */
	private void refreshProject(IProject project, IProgressMonitor monitor)
			throws SVNException {
	    if (monitor != null)
	    {
	    	monitor.beginTask("", 100); //$NON-NLS-1$
	    }
		try {
			// Register the project with Team
			RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
			RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId());
		} catch (TeamException e) {
			throw new SVNException("Cannot map the project with svn provider",e);
		} finally {
			if (monitor != null)
			{
				monitor.done();
			}
		}
	}	
    
}
