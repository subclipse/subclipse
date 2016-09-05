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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Update the given resources in the given project to the given revision
 * 
 * @author Cedric Chabanois (cchab at tigris.org)
 */
public class UpdateResourcesCommand implements ISVNCommand {
    private SVNWorkspaceRoot root;
    private IResource[] resources;
    private SVNRevision revision; 
    private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
    private boolean setDepth = false;
    private boolean ignoreExternals = false;
    private boolean force = true;
    private Set<IResource> updatedResources = new LinkedHashSet<IResource>();
    private ISVNConflictResolver conflictResolver;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();
    
    /**
     * Update the given resources.
     * BEWARE ! The resource array has to be sorted properly, so parent folder (incoming additions) are updated sooner than their children.
     * BEWARE ! For incoming deletions, it has to be opposite. 
     * WATCH OUT ! These two statements mean that you CANNOT have both additions and deletions within the same call !!!
     * When doing recursive call, it's obviously not an issue ... 
     * @param root
     * @param resources
     * @param revision
     */
    public UpdateResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, SVNRevision revision) {
        this.root = root;
        this.resources = resources;
        this.revision = revision;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IProgressMonitor monitor) throws SVNException {
		final ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
		OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor, svnClient));		
		try {		
	        List<IProject> projectList = new ArrayList<IProject>();
	        for (IResource currentResource : resources) {
	        	IProject project = currentResource.getProject();
	        	if (!projectList.contains(project)) {
	        		projectList.add(project);
	        	}        	
	        }
			if (conflictResolver != null) {
				svnClient.addConflictResolutionCallback(conflictResolver);
			}
			
	        IProject[] projects = new IProject[projectList.size()];
	        projectList.toArray(projects);
	        ISchedulingRule rule = MultiRule.combine(projects);
	        
	        SVNProviderPlugin.run(new ISVNRunnable() {
	            public void run(final IProgressMonitor pm) throws SVNException {
	                try {
	                    monitor.beginTask(null, 100 * resources.length);                    
	
	                    svnClient.addNotifyListener(operationResourceCollector);
	                    
	            		if (resources.length == 1)
	            		{
	                        monitor.subTask(resources[0].getName());
	                        svnClient.update(resources[0].getLocation().toFile(),revision, depth, setDepth, ignoreExternals, force);
	                        updatedResources.add(resources[0]);
	                        monitor.worked(100);    			
	            		}
	            		else
	            		{
	            			File[] files = new File[resources.length];
	            			for (int i = 0; i < resources.length; i++) {
	        					files[i] = resources[i].getLocation().toFile();
	        					updatedResources.add(resources[i]);
	        				}
	          
	           				svnClient.update(files, revision, depth, setDepth, ignoreExternals, force);   				
	           				monitor.worked(100);
	            		}
	                } catch (SVNClientException e) {
	                    throw SVNException.wrapException(e);
	                } finally {
	                	monitor.done();
	                	if (svnClient != null) {
		            		if (conflictResolver != null) {
		            			svnClient.addConflictResolutionCallback(null);
		            		}
		            		svnClient.removeNotifyListener(operationResourceCollector);
		            		root.getRepository().returnSVNClient(svnClient);
	                	}
	                }                    	
	            }
	        }, rule, Policy.monitorFor(monitor));
        } finally {
        	OperationManager.getInstance().endOperation(true, operationResourceCollector.getOperationResources());
        }
	}

	public void setConflictResolver(ISVNConflictResolver conflictResolver) {
		this.conflictResolver = conflictResolver;
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
    
}