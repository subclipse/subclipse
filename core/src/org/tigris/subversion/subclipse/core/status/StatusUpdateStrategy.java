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
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

/**
 * When the status of a resource is asked, we don't update a resource at once.
 * We use a strategy to get the status of several resources at once.
 * There are more than one strategy. All strategies inherit from this class
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public abstract class StatusUpdateStrategy {
	protected StatusCacheComposite treeCacheRoot;
    	
	public StatusUpdateStrategy(StatusCacheComposite treeCacheRoot)
	{
		super();
		setTreeCacheRoot(treeCacheRoot);
	}
	
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
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        for (int i = 0; i < statuses.length;i++) {
            updateCache(new LocalResourceStatus(statuses[i]), workspaceRoot);
        }
    }
    
    /**
     * update the cache using the given statuses
     * @param statuses
     */
    protected void updateCache(LocalResourceStatus[] statuses) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();        
        for (int i = 0; i < statuses.length;i++) {
            updateCache(statuses[i], workspaceRoot);
        }
    }

    /**
     * update the cache using the given statuses
     * @param status
     * @param workspaceRoot
     */
    protected void updateCache(LocalResourceStatus status, IWorkspaceRoot workspaceRoot) {
    	
    	IPath resourcePath = SVNWorkspaceRoot.pathForLocation(status.getPath());
    	
    	if (resourcePath != null) {
    		treeCacheRoot.addStatus(resourcePath, status);
    	}
    }
}
