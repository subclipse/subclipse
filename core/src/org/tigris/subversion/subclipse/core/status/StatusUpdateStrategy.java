/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.status;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * When the status of a resource is asked, we don't update a resource at once.
 * We use a strategy to get the status of several resources at once.
 * There are more than one strategy. All strategies inherit from this class
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public abstract class StatusUpdateStrategy {
	protected StatusCacheComposite treeCacheRoot;
    
	/**
	 * @param treeCacheRoot The treeCacheRoot to set.
	 */
	public void setTreeCacheRoot(StatusCacheComposite treeCacheRoot) {
		this.treeCacheRoot = treeCacheRoot;
	}

    /**
     * update the status of the given resource and possibly to other resources 
     * as well depending on the strategy
     * @param resource
     * @throws SVNException
     */
    abstract void updateStatus(IResource resource) throws SVNException;

    /**
     * update the cache using the given statuses
     * @param statuses
     */
    protected void updateCache(ISVNStatus[] statuses) {
        LocalResourceStatus[] localResourceStatuses = new LocalResourceStatus[statuses.length];
        for (int i = 0; i < statuses.length;i++) {
            localResourceStatuses[i] = new LocalResourceStatus(statuses[i]);
        }
        updateCache(localResourceStatuses);
    }
    
    /**
     * update the cache using the given statuses
     * @param statuses
     */
    protected void updateCache(LocalResourceStatus[] statuses) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        
        for (int i = 0; i < statuses.length;i++) {
            LocalResourceStatus status = statuses[i];

            IPath pathEclipse = new Path(status.getFile().getAbsolutePath());
                
            IResource resourceStatus = null;
            
            // we can't test using file.isDirectory and file.isFile because both return false when
            // the resource has been deleted
            if (status.getNodeKind().equals(SVNNodeKind.DIR)) {
                resourceStatus = workspaceRoot.getContainerForLocation(pathEclipse);
            }
            else {
                resourceStatus = workspaceRoot.getFileForLocation(pathEclipse);
            }
            
            if (resourceStatus != null) {
                treeCacheRoot.addStatus(resourceStatus, status);
            }
        }
    }
}
