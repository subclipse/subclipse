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
package org.tigris.subversion.subclipse.core.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Remove stale transactions and locks from the working copy.  This operation can only be run
 * on folders.
 */
public class CleanupResourcesCommand implements ISVNCommand {

    private final SVNWorkspaceRoot root;
    private final IResource[] resources;
    private Set<IResource> cleanedUpResources = new LinkedHashSet<IResource>();

    public CleanupResourcesCommand(SVNWorkspaceRoot root, IResource[] resources) {
        this.root = root;
        this.resources = resources;
        
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
    	ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
        try {
            monitor.beginTask(null, 100 * resources.length);            
            OperationManager.getInstance().beginOperation(svnClient);           
            for (int i = 0; i < resources.length; i++) {
            	if (resources[i].getLocation() != null) {
	                svnClient.cleanup(resources[i].getLocation().toFile());
	                cleanedUpResources.add(resources[i]);
            	}
                monitor.worked(100);
            }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
    		Set<IResource> refreshResources = new LinkedHashSet<IResource>();
    		for (IResource resource : cleanedUpResources) {
    			addToRefreshList(refreshResources, resource);
    		}
    		OperationManager.getInstance().endOperation(true, refreshResources);
            root.getRepository().returnSVNClient(svnClient);
            monitor.done();
        }
    }

	private void addToRefreshList(Set<IResource> refreshResources, IResource resource) {
		refreshResources.add(resource);
		OperationManager.getInstance().onNotify(resource.getLocation().toFile(), null);
		if (resource instanceof IContainer) {
			try {
				IResource[] children = ((IContainer)resource).members();
				for (IResource child : children) {
					if (child instanceof IContainer && child.getLocation() != null) {
						addToRefreshList(refreshResources, child);
					}
				}
			} catch (CoreException e) {}
		}
	} 
}
