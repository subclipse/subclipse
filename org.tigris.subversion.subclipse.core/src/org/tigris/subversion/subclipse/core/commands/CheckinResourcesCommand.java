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
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * Checkin any local changes to given resources in the given project
 * 
 * @author Cedric Chabanois (cchab at tigris.org) 
 */
public class CheckinResourcesCommand implements ISVNCommand {
	// resources to commit
	protected IResource[] resources;
    
    protected String message;
    
    protected boolean keepLocks;
    
    protected int depth;
    
    protected SVNWorkspaceRoot root;
    
    private ISVNNotifyListener notifyListener;
    
    private String postCommitError;
    
    private boolean commitError;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();

    public CheckinResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, int depth, String message, boolean keepLocks) {
    	this.resources = resources;
        this.message = message;
        this.depth = depth;
        this.root = root;
        this.keepLocks = keepLocks;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
		commitError = false;
		postCommitError = null;
        final ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
        
        OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor, svnClient));
        
        try {
        // Prepare the parents list
        // we will Auto-commit parents if they are not already commited
        List<IContainer> parentsList = new ArrayList<IContainer>();
        List<IProject> projectList = new ArrayList<IProject>();
        for (IResource currentResource : resources) {
        	
        	IProject project = currentResource.getProject();
        	if (!projectList.contains(project)) {
        		projectList.add(project);
        	}
        	
            IContainer parent = currentResource.getParent();
            ISVNLocalResource svnParentResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
            while (parent.getType() != IResource.ROOT && 
                   parent.getType() != IResource.PROJECT && 
                   !svnParentResource.hasRemote()) {
            	if (!inCommitList(parent))
            		parentsList.add(parent);
                parent = parent.getParent();
                svnParentResource = svnParentResource.getParent();
            }
        }
        
        // convert parents and resources to an array of File
        int parents = parentsList.size();
        if (parents > 0)
        	depth = IResource.DEPTH_ZERO; // change commit to non-recursive!!
           
        final File[] resourceFiles = new File[parents + resources.length];
        for (int i = 0; i < parents; i++) {
        	resourceFiles[i] = ((IResource)parentsList.get(i)).getLocation().toFile();
        }
        for (int i = 0, j = parents; i < resources.length; i++, j++) {
            resourceFiles[j] = resources[i].getLocation().toFile();  
        }
        
        IProject[] projects = new IProject[projectList.size()];
        projectList.toArray(projects);
        ISchedulingRule rule = MultiRule.combine(projects);
        
        SVNProviderPlugin.run(new ISVNRunnable() {
            public void run(final IProgressMonitor pm) throws SVNException {
                try {             	
                    notifyListener = new ISVNNotifyListener() {
            			public void logCommandLine(String commandLine) {}
            			public void logCompleted(String message) {}
            			public void logError(String message) {}
            			public void logMessage(String message) {
            				if (message.startsWith("Transmitting file data"))
            					pm.subTask(message);
            			}
            			public void logRevision(long revision, String path) {}
            			public void onNotify(File path, SVNNodeKind kind) {}
            			public void setCommand(int command) {}     	
                    };   
                	
                    pm.beginTask(null, resourceFiles.length);
                    pm.setTaskName("Checking in...");
                    
                    svnClient.addNotifyListener(operationResourceCollector);
                    
                    svnClient.addNotifyListener(notifyListener);
                    
                    // then the resources the user has requested to commit
                    if (svnClient.canCommitAcrossWC()) svnClient.commitAcrossWC(resourceFiles,message,depth == IResource.DEPTH_INFINITE,keepLocks,true);
                    else svnClient.commit(resourceFiles,message,depth == IResource.DEPTH_INFINITE,keepLocks);
                    postCommitError = svnClient.getPostCommitError();
                } catch (SVNClientException e) {
                	commitError = true;
                    throw SVNException.wrapException(e);
                } finally {             	
                    pm.done();
                    if (svnClient != null) {
	                    svnClient.removeNotifyListener(operationResourceCollector);
	                    svnClient.removeNotifyListener(notifyListener);                 
	                    root.getRepository().returnSVNClient(svnClient);
                    }
                }
            }
        }, rule, Policy.monitorFor(monitor));
        } finally {
        	OperationManager.getInstance().endOperation(true, operationResourceCollector.getOperationResources(), !commitError);
        }
	}
    
	private boolean inCommitList(IResource resource) {
		for (IResource checkResource : resources) {
			if (checkResource.equals(resource))
				return true;
		}
		return false;
	}

	public String getPostCommitError() {
		return postCommitError;
	}
	
}
