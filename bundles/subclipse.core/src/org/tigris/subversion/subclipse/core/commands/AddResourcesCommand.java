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

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Add the given resources to the project. 
 * <p>
 * The sematics follow that of SVN in the sense that any folders and files
 * are created remotely on the next commit. 
 * </p>
 * 
 * @author Cedric Chabanois (cchab at tigris.org)
 */
public class AddResourcesCommand implements ISVNCommand {
	// resources to add
    private IResource[] resources; 
    
    private int depth;
    
    private SVNWorkspaceRoot root;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();
    
    public AddResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, int depth) {
        this.resources = resources;
        this.depth = depth;
        this.root = root;
    }
    
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
        monitor = Policy.monitorFor(monitor);
        
        // Visit the children of the resources using the depth in order to
        // determine which folders, text files and binary files need to be added
        // A TreeSet is needed for the folders so they are in the right order (i.e. parents created before children)
        final SortedSet<ISVNLocalResource> folders = new TreeSet<ISVNLocalResource>();
        // Sets are required for the files to ensure that files will not appear twice if there parent was added as well
        // and the depth isn't zero
        final HashSet<ISVNLocalResource> files = new HashSet<ISVNLocalResource>();
        
        for (int i=0; i<resources.length; i++) {
            
            final IResource currentResource = resources[i];
            
            try {       
                // Auto-add parents if they are not already managed
                IContainer parent = currentResource.getParent();
                ISVNLocalResource svnParentResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
                while (parent.getType() != IResource.ROOT && parent.getType() != IResource.PROJECT && ! svnParentResource.isManaged()) {
                    folders.add(svnParentResource);
                    parent = parent.getParent();
                    svnParentResource = svnParentResource.getParent();
                }
                    
                // Auto-add children accordingly to depth
                final SVNException[] exception = new SVNException[] { null };
                currentResource.accept(new IResourceVisitor() {
                    public boolean visit(IResource resource) {
                        try {
                            ISVNLocalResource mResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
                            // Add the resource is its not already managed and it was either
                            // added explicitly (is equal currentResource) or is not ignored
                            if ((! mResource.isManaged()) && (currentResource.equals(resource) || ! mResource.isIgnored())) {
                                if (resource.getType() == IResource.FILE) {
                                    files.add(mResource);
                                } else {
                                    folders.add(mResource);
                                }
                            }
                            // Always return true and let the depth determine if children are visited
                            return true;
                        } catch (SVNException e) {
                            exception[0] = e;
                            return false;
                        }
                    }
                }, depth, false);
                if (exception[0] != null) {
                    throw exception[0];
                }
            } catch (CoreException e) {
                throw new SVNException(new Status(IStatus.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNTeamProvider.visitError", new Object[] {resources[i].getFullPath()}), e)); //$NON-NLS-1$
            }
        } // for
        // If an exception occured during the visit, throw it here

        // Add the folders, followed by files!
        ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
        monitor.beginTask(null, files.size() + folders.size());
        monitor.setTaskName("Adding...");
        
        svnClient.addNotifyListener(operationResourceCollector);
        
        OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor, svnClient));
        try {
            for(ISVNLocalResource localResource : folders) {
                try {
                    svnClient.addDirectory(localResource.getIResource().getLocation().toFile(),false);
                    localResource.refreshStatus();
                } catch (SVNClientException e) {
                    throw SVNException.wrapException(e);
                }
            }

            for(ISVNLocalResource localResource : files) {
                try {
                    svnClient.addFile(localResource.getIResource().getLocation().toFile());
                    // If file has read-only attribute set, remove it
                    ResourceAttributes attrs = localResource.getIResource().getResourceAttributes();
                    if (localResource.getIResource().getType() == IResource.FILE && attrs.isReadOnly()) {
                        attrs.setReadOnly(false);
                    	try {
							localResource.getIResource().setResourceAttributes(attrs);
						} catch (CoreException swallow) {
						}
                    }
                } catch (SVNClientException e) {
                    throw SVNException.wrapException(e);
                }    
            }
                

        } finally {
        	Set<IResource> operationResources = operationResourceCollector.getOperationResources();
            OperationManager.getInstance().endOperation(true, operationResources);
            monitor.done();
            if (svnClient != null) {
	            svnClient.removeNotifyListener(operationResourceCollector);
	            root.getRepository().returnSVNClient(svnClient);
            }
        }
    }
    
    
}
