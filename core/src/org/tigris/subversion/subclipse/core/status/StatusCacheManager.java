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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.status.StatusCacheComposite;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNStatusUnversioned;

/**
 * Provides a method to get the status of a resource. <br>   
 * It is much more efficient to get the status of a set a resources than only
 * one resource at a time. For that we use a @link org.tigris.subversion.subclipse.core.status.StatusUpdateStrategy<br>
 * 
 * We use a tree (@link org.tigris.subversion.subclipse.core.status.StatusCacheComposite) to keep the status of the resources  
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public class StatusCacheManager implements Preferences.IPropertyChangeListener {

	/** Name used for identifying SVN synchronization data in Resource>ResourceInfo#syncInfo storage */
	public static final QualifiedName SVN_BC_SYNC_KEY = new QualifiedName(SVNProviderPlugin.ID, "svn-bc-sync-key");

    private IStatusCache statusCache;
    private StatusUpdateStrategy statusUpdateStrategy;
    
    public StatusCacheManager() {
		ResourcesPlugin.getWorkspace().getSynchronizer().add(StatusCacheManager.SVN_BC_SYNC_KEY);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.internal.resources.IManager#startup(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void startup(IProgressMonitor monitor) throws CoreException {
    	//TODO originally the cache status preference was used to switch between new and old impl.
        //loadStatusCache();
    	statusCache = new SynchronizerSyncInfoCache();
        chooseUpdateStrategy();
    }

    /**
  	 * @deprecated should be removed when StatusCacheComposite will be definitely replaced by SynchronizerSyncInfoCache
     */
    private void loadStatusCache() {
        File statusCacheFile = new File(SVNProviderPlugin.getPlugin().getStateLocation() + File.separator + "status.cache");
        if (SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_CACHE_STATUS) && statusCacheFile.exists()) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(statusCacheFile));
                statusCache = (IStatusCache)in.readObject();
                in.close();
                statusCacheFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
                statusCache = new StatusCacheComposite();
            }
        } else statusCache = new SynchronizerSyncInfoCache();
    }

    private void chooseUpdateStrategy() {
        boolean recursiveStatusUpdate = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_RECURSIVE_STATUS_UPDATE);
        statusUpdateStrategy = recursiveStatusUpdate ? (StatusUpdateStrategy)new RecursiveStatusUpdateStrategy(statusCache) : (StatusUpdateStrategy)new NonRecursiveStatusUpdateStrategy(statusCache);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.core.internal.resources.IManager#shutdown(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void shutdown(IProgressMonitor monitor) throws CoreException {
        if (SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_CACHE_STATUS))
            saveStatusCache();
    }
    
    /**
  	 * @deprecated should be removed when StatusCacheComposite will be definitely replaced by SynchronizerSyncInfoCache
     */
    private void saveStatusCache() {
        File statusCacheFile = new File(SVNProviderPlugin.getPlugin().getStateLocation() + File.separator + "status.cache");
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(statusCacheFile));
            out.writeObject(statusCache);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A resource which ancestor is not managed is not managed
     * @param resource
     * @return true if an ancestor of the resource is not managed and false 
     *         if we don't know 
     */
    private boolean isAncestorNotManaged(IResource resource) {
        IResource parent = resource.getParent();
        if (parent == null) {
            return false;
        }
        
        while (parent != null) {
            LocalResourceStatus statusParent = statusCache.getStatus(parent);
            
            if (statusParent != null) {
            	if (!statusParent.isManaged()) {
            		return true;
            	}
            }
            parent = parent.getParent();
        }
        return false;
    }
    
    /**
     * update the cache using the given statuses
     * @param statuses
     */
    protected IPath[] updateCache(ISVNStatus[] statuses) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath[] result = new IPath[statuses.length];
        for (int i = 0; i < statuses.length;i++) {        	
        	result[i] = updateCache(new LocalResourceStatus(statuses[i]), workspaceRoot);
        }
        return result;
    }
    
    /**
     * update the cache using the given statuses
     * @param statuses
     */
    protected IPath[] updateCache(LocalResourceStatus[] statuses) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath[] result = new IPath[statuses.length];
        for (int i = 0; i < statuses.length;i++) {
            result[i] = updateCache(statuses[i], workspaceRoot);
        }
        return result;
    }

    /**
     * update the cache using the given statuses
     * @param status
     * @param workspaceRoot
     */
    protected IPath updateCache(LocalResourceStatus status, IWorkspaceRoot workspaceRoot) {
    	
    	IPath resourcePath = SVNWorkspaceRoot.pathForLocation(status.getPath());
    	
    	if (resourcePath != null) {
    		statusCache.addStatus(resourcePath, status);
    	}
    	
    	return resourcePath;
    }

    /**
     * Get the status of the given resource.
     * If the status is not present in cache, it will be retrieved using the actual updateStrategy.
     * If recursive startegy is being used also all child resources would be updated,
     * otherwise only direct childern
     * @param resource whose status is required.
     *   
     * @throws SVNException
     */
    public LocalResourceStatus getStatus(IResource resource) throws SVNException {
        return getStatus(resource, statusUpdateStrategy);
    }

    /**
     * Get the status of the given resource.
     * If the status is not present in cache, it will be retrieved using the specified updateStrategy.
     * If recursive startegy is being used also all child resources would be updated,
     * otherwise only direct childern
     *
     * @param resource whose status is required.
     * @param useRecursiveStartegy when true also children statuses should be recursively updated 
     * @throws SVNException
     */
    public LocalResourceStatus getStatus(IResource resource, boolean useRecursiveStartegy) throws SVNException {
        return getStatus(resource,
				useRecursiveStartegy ? 
						(StatusUpdateStrategy) new RecursiveStatusUpdateStrategy(statusCache)
						: (StatusUpdateStrategy) new NonRecursiveStatusUpdateStrategy(statusCache));
    }

    /**
     * get the status of the given resource
     * @throws SVNException
     */
    private LocalResourceStatus getStatus(IResource resource, StatusUpdateStrategy strategy) throws SVNException {
        LocalResourceStatus status = null;

        status = statusCache.getStatus(resource);
        
        // we get it using svn 
        if (status == null)
        {
        	status = basicGetStatus(resource, strategy);
        }
        
        ensureBaseStatusInfo(resource);
        
        return status;
    }
    
    /**
     * Get the statuse(s) from the svn meta files
     * 
     * @param resource
     * @param strategy
     * @return
     * @throws SVNException
     */
    private LocalResourceStatus basicGetStatus(IResource resource, StatusUpdateStrategy strategy) throws SVNException 
	{
        LocalResourceStatus status = null;

        if (isAncestorNotManaged(resource)) {
            // we know the resource is not managed because one of its ancestor is not managed
       		status = new LocalResourceStatus(new SVNStatusUnversioned(resource.getLocation().toFile(),false)); 
        } else {
            // we don't know if resource is managed or not, we must update its status
        	strategy.setStatusCache(statusCache);
        	setStatuses(strategy.statusesToUpdate(resource));
        	status = statusCache.getStatus(resource);
        }
        
        if (status == null) {
            status = new LocalResourceStatus(new SVNStatusUnversioned(resource.getLocation().toFile(),false));
        }
        
        return status;
    }

    /**
     * The cache manager handles itself the status retrieving. However this method can
     * be used to update the statuses of some resources  
     * 
     * @param statuses
     */
    public void setStatuses(LocalResourceStatus[] statuses) {
        updateCache(statuses);
    }

    /**
     * The cache manager handles itself the status retrieving. However this method can
     * be used to update the statuses of some resources  
     * 
     * @param statuses
     */
    public void setStatuses(ISVNStatus[] statuses) {
        updateCache(statuses);
    }

    /**
     * refresh the status for the given resource
     * @param resource
     * @param depth
     * @throws SVNException
     */
    public void refreshStatus(IResource resource,int depth) throws SVNException {
    	StatusUpdateStrategy strategy = 
    		(depth == IResource.DEPTH_INFINITE) 
							? (StatusUpdateStrategy) new RecursiveStatusUpdateStrategy(statusCache)
							: (StatusUpdateStrategy) new NonRecursiveStatusUpdateStrategy(statusCache);
		try {		
			Map resourcesToRefresh = resourcesToRefresh(resource, depth);
			IPath[] refreshedPaths = updateCache(strategy.statusesToUpdate(resource));
			for (int i = 0; i < refreshedPaths.length; i++) {
				resourcesToRefresh.remove(refreshedPaths[i]);
			}
			//Resources which were not refreshed above (e.g. deleted resources)
			for (Iterator it = resourcesToRefresh.values().iterator(); it.hasNext();) {
				statusCache.addStatus((IResource) it.next(), null);
			}
		}
		catch (CoreException e)
		{
			throw SVNException.wrapException(e);
		}
    }

    private Map resourcesToRefresh(IResource resource, int depth) throws CoreException
    {
    	final Map resultSet = new HashMap();
		resource.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				resultSet.put(resource.getFullPath(), resource);
				return true;
			}
		}, depth, true);
		return resultSet;
    }
    
    /**
 	 * @deprecated should be removed when StatusCacheComposite will be definitely replaced by SynchronizerSyncInfoCache
     */
    public void refreshStatusAndBaseInfo(IResource resource) throws SVNException
    {
    	refreshStatus(resource, IResource.DEPTH_INFINITE);
    	statusCache.ensureBaseStatusInfo(resource, IResource.DEPTH_INFINITE);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (ISVNCoreConstants.PREF_RECURSIVE_STATUS_UPDATE.equals(event.getProperty()))  {
            chooseUpdateStrategy();
        }
    }
    
	/**
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#ensureBaseStatusInfo(org.eclipse.core.resources.IResource, org.tigris.subversion.subclipse.core.resources.LocalResourceStatus)
 	 * @deprecated should be removed when StatusCacheComposite will be definitely replaced by SynchronizerSyncInfoCache
	 */
	public void ensureBaseStatusInfo(IResource resource) throws SVNException
	{
		this.statusCache.ensureBaseStatusInfo(resource);
	}		

}
