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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.status.IStatusCache;
import org.tigris.subversion.subclipse.core.status.StatusCacheManager;

/**
 * We use a tree to cache LocalResourceStatus for each resource
 * We could use :
 * - {@link IResource#setSessionProperty} 
 * but it can only be used existing resources
 * - {@link ISynchronizer#setSyncInfo}
 * but it cannot be used in a {@link IResourceChangeListener} because it modified
 * workspace itself
 * 
 * @author cedric chabanois (cchab at tigris.org) 
 */
public class StatusCacheComposite implements IStatusCache, Serializable {
	private String segment;
    private LocalResourceStatus status = null;
    private Map children = null;
    static final long serialVersionUID = 1L;

    /**
     * creates the root of the tree cache
     *
     */
    public StatusCacheComposite() {
    }
    
    private StatusCacheComposite(String segment, LocalResourceStatus status) {
        this.segment = segment;
        this.status = status;
    }
        
    
    /**
     * add a status
     * @param segment
     * @param status
     * @return
     */
    synchronized private StatusCacheComposite addStatus(String segment, LocalResourceStatus status) {
    	if (children == null) {
    		children = new HashMap();
        }
        StatusCacheComposite child = (StatusCacheComposite)children.get(segment);
        if (child == null) {
        	child = new StatusCacheComposite(segment,status); 
        	children.put(segment, child);
        } else {
        	child.status = status;
        }
        return child;
    }
    
    
    /**
     * get the given child or null if this composite has no children with this name
     * @param segment
     * @return
     */
    synchronized private StatusCacheComposite getChild(String segment) {
        if (children == null) {
        	return null;
        }
        return (StatusCacheComposite)children.get(segment);
    }
    
    /**
     * add a status for the given resource (which does not need to exist)
     * @param resource
     * @param status
     */
    synchronized public void addStatus(IResource resource, LocalResourceStatus status) {
    	addStatus(resource.getFullPath(),status);
    	setBaseStatusInfo(resource, status);
    }
    
    /**
     * add a status at the given relative path
     * @param path
     * @param status
     */
    synchronized public void addStatus(IPath path, LocalResourceStatus status) {
    	if (path.segmentCount() == 0) {
    		return;
    	} else if (path.segmentCount() == 1) {
    		addStatus(path.lastSegment(), status);
        } else {
        	StatusCacheComposite child = getChild(path.segment(0));
            if (child == null) {
            	child = addStatus(path.segment(0),null);
            }
            child.addStatus(path.removeFirstSegments(1), status);
        }
    }
    
    /**
     * get the status of the given resource (which does not need to exist)
     * @param resource
     * @return
     */
    synchronized public LocalResourceStatus getStatus(IResource resource) {
    	return getStatus(resource.getFullPath());
    }
        
    /**
     * get the status at the given relative path
     * @param path
     * @return
     */
    synchronized private LocalResourceStatus getStatus(IPath path) {
        if (path.segmentCount() == 0) {
        	return status;
        }
        StatusCacheComposite child = getChild(path.segment(0));
        if (child == null) {
        	return null;
        } else {
        	return child.getStatus(path.removeFirstSegments(1));
        }
    }
    
    /**
     * refersh a child status
     * @param segment
     * @param depth
     */
    synchronized private void refreshChildStatus(String segment, int depth) {
    	StatusCacheComposite child = getChild(segment);
        if (child == null) {
        	return;
        }
        if (child.children == null) {
            children.remove(segment);
        } else {
        	child.status = null;
        }
        
        if (depth == IResource.DEPTH_ONE) {
        	if (child.children != null) {
        		for (Iterator it = child.children.values().iterator(); it.hasNext(); ) {
        			StatusCacheComposite grandchild = (StatusCacheComposite)it.next();                    
                    grandchild.status = null;
                }
            }
        }
        
        if (depth == IResource.DEPTH_INFINITE)  {
        	child.children = null;
        }
    }
    
    /**
     * refresh the status for the given resource (which does not need to exist)
     * @param resource
     * @param depth
     */
    synchronized public void refreshStatus(IResource resource, int depth) {
    	refreshStatus(resource.getFullPath(),depth);
    }
    
    /**
     * refreshes the status at the given relative path
     * @param path
     * @param depth
     */
    synchronized private void refreshStatus(IPath path, int depth) {
    	if (path.segmentCount() > 1) {
            StatusCacheComposite child = getChild(path.segment(0));
            if (child == null) {
            	return;
            }
    		child.refreshStatus(path.removeFirstSegments(1),depth);
        } else {
            StatusCacheComposite child = getChild(path.segment(0));
            if (child == null) {
                return;
            } 
            refreshChildStatus(path.segment(0),depth);
        }
    }
    
    /**
     * Set the baseCopy status info to Resource's syncoInfo storage.
     * (This is necessary for e.g. phantom resources to work properly)  
     * @param resource
     */
	public void setBaseStatusInfo(IResource resource, LocalResourceStatus status)
	{
		ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();
		try {
			if (status.hasRemote()) {
				synchronizer.setSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource, status.getBytes());
			} else {
				if (synchronizer.getSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource) != null)
				{
					if (!SVNWorkspaceRoot.isSvnMetaResource(resource))
					{
						synchronizer.setSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource, null);
					}
				}
			}
		}
		catch (CoreException e)
		{
			SVNProviderPlugin.log(SVNException.wrapException(e));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#ensureBaseStatusInfo(org.eclipse.core.resources.IResource)
	 */
	public void ensureBaseStatusInfo(IResource resource) throws SVNException
	{
		ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();
		LocalResourceStatus status = getStatus(resource);
		try {
			if( synchronizer.getSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource) == null ) {
				if( (status != null) && status.hasRemote() ) {
					synchronizer.setSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource, status.getBytes());
				}
			}
			else
			{
				if( (status != null) && !status.hasRemote() && (!SVNWorkspaceRoot.isSvnMetaResource(resource))) 
				{
					//This should not normally happen, but just to be sure ...
					synchronizer.setSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource, null);
				}
			}
		}
		catch (CoreException e)
		{
			throw SVNException.wrapException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#ensureBaseStatusInfo(org.eclipse.core.resources.IResource, int)
	 */
	public void ensureBaseStatusInfo(IResource resource, int depth) throws SVNException
	{
		try {
			resource.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					ensureBaseStatusInfo(resource);
					return true;
				}
			});
		}		catch (CoreException e)
		{
			throw SVNException.wrapException(e);
		}
	}

}
