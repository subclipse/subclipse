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
package org.tigris.subversion.subclipse.core.status;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;

/**
 * Local sync info cache using ResourceInfo.syncInfo for storage.
 * 
 */
public class SynchronizerSyncInfoCache implements IStatusCache {
	
	protected static final byte[] BYTES_REMOVED = new byte[0];
	protected SyncInfoSynchronizedAccessor accessor = new SyncInfoSynchronizedAccessor();
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#hasCachedStatus(org.eclipse.core.resources.IResource)
	 */
	public boolean hasCachedStatus(IResource resource)
	{
		try {
			return getCachedSyncBytes(resource) != null;
		} catch (SVNException e) {
			SVNProviderPlugin.log(e);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#getStatus(org.eclipse.core.resources.IResource)
	 */
	public LocalResourceStatus getStatus(IResource resource){
		try {
			return LocalResourceStatus.fromBytes(getCachedSyncBytes(resource));
		} catch (SVNException e) {
			SVNProviderPlugin.log(e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#addStatus(org.tigris.subversion.subclipse.core.resources.LocalResourceStatus)
	 */
	public IResource addStatus(IResource resource, LocalResourceStatus status) {
		try {
			if (resource == null) return null;
			if (status.isUnversioned() && !(resource.exists() || resource.isPhantom()))
			{
				return resource;
			}
			setCachedSyncBytes(resource, status.getBytes());
			return resource;
		} catch (SVNException e) {
		    if (!"".equals(e.getMessage())) // We send these exceptions so that the log does not go nuts
		        SVNProviderPlugin.log(e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#removeStatus(org.eclipse.core.resources.IResource)
	 */
	public IResource removeStatus(IResource resource)
	{
		try {			
			setCachedSyncBytes(resource, null);
			return resource;
		} catch (SVNException e)
		{
			SVNProviderPlugin.log(e);
			return null;			
		}
	}
	
	private byte[] getCachedSyncBytes(IResource resource) throws SVNException {
		try {
			byte[] bytes;
			if (accessor.pendingCacheContains(resource)) {
				bytes = accessor.readFromPendingCache(resource);
				if (bytes == BYTES_REMOVED) {
					bytes = null;
				}
			} else {
				bytes = accessor.internalGetCachedSyncBytes(resource);
			}
			return bytes;
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		}
	}
	
	private void setCachedSyncBytes(IResource resource, byte[] syncBytes) throws SVNException {
		byte[] oldBytes = getCachedSyncBytes(resource);
		try {
			if (syncBytes == null) {
				if (oldBytes != null) {
					boolean canModifyWorkspace = !ResourcesPlugin.getWorkspace().isTreeLocked();
					if (canModifyWorkspace) {
						try {
							accessor.removeFromPendingCache(resource);
							if (resource.exists() || resource.isPhantom()) {
								accessor.internalSetCachedSyncBytes(resource, null);
							}
						} catch (Exception e) {
							if (resource.exists() || resource.isPhantom()) {
								accessor.writeToPendingCache(resource, BYTES_REMOVED);
							}							
						}
					} else {
						if (resource.exists() || resource.isPhantom()) {
							accessor.writeToPendingCache(resource, BYTES_REMOVED);
						}
					}
				}
			} else {
				// ensure that the sync info is not already set to the same thing.
				// We do this to avoid causing a resource delta when the sync info is 
				// initially loaded (i.e. the synchronizer has it and so does the Entries file
				if (oldBytes == null || !SyncInfoSynchronizedAccessor.equals(syncBytes, oldBytes)) {
					boolean canModifyWorkspace = !ResourcesPlugin.getWorkspace().isTreeLocked();
					if (canModifyWorkspace) {
						try {
							accessor.removeFromPendingCache(resource);
							accessor.internalSetCachedSyncBytes(resource, syncBytes);
						} catch (Exception e) {
							LocalResourceStatus.fromBytes(oldBytes);
							accessor.writeToPendingCache(resource, syncBytes);							
						}
					} else {
						LocalResourceStatus.fromBytes(oldBytes);
						accessor.writeToPendingCache(resource, syncBytes);
					}
				}
			}
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#purgeCache(org.eclipse.core.resources.IContainer, boolean)
	 */
	public void purgeCache(IContainer root, boolean deep) throws SVNException {
		int depth = deep ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO;
		try {
			if (root.exists() || root.isPhantom()) {
				ResourcesPlugin.getWorkspace().getSynchronizer().flushSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, root, depth);
			}
			if (deep) {
				accessor.removeRecursiveFromPendingCache(root);
			} else {
				accessor.removeFromPendingCache(root);
			}
		} catch (CoreException e) {
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
				// Must have been deleted since we checked
				return;
			}
			throw SVNException.wrapException(e);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.IStatusCache#flushPendingStatuses()
	 */
	public void flushPendingStatuses()
	{
		if (accessor.isFlushFeasible())
		{
			try {
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) {
						accessor.flushPendingCacheWrites();
					}
				}, null);
			} catch (CoreException e) {
				SVNProviderPlugin.log(SVNException.wrapException(e));
			}
		}
	}
	
	protected final static class SyncInfoSynchronizedAccessor
	{
		// Map of sync bytes that were set without a scheduling rule
		private Map<IResource, byte[]> pendingCacheWrites = new HashMap<IResource, byte[]>();

		/*
		 * Retieve the cached sync bytes from the synchronizer. A null
		 * is returned if there are no cached sync bytes.
		 */
		protected byte[] internalGetCachedSyncBytes(IResource resource) throws SVNException {
			try {
				return ResourcesPlugin.getWorkspace().getSynchronizer().getSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource);
			} catch (CoreException e) {
				throw SVNException.wrapException(e);
			}
		}

		/*
		 * Set the sync bytes to the synchronizer.
		 */
		protected void internalSetCachedSyncBytes(IResource resource, byte[] syncInfo) throws SVNException {
			try {
				ResourcesPlugin.getWorkspace().getSynchronizer().setSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource, syncInfo);
			} catch (CoreException e) {
				throw SVNException.wrapException(e);
			}
		}

		/**
		 * Flushes statuses from pending cache.
		 * The method is not synchronized intentionally.
		 */
		protected void flushPendingCacheWrites()
		{
			if ((pendingCacheWrites.size() > 0) && (!ResourcesPlugin.getWorkspace().isTreeLocked()))
			{
				int count = pendingCacheWrites.size();
				for (int i = 0; i < count; i++) {
					Map.Entry cachedEntry = nextFromPendingCache();
					if (cachedEntry != null)
					{
						IResource resource = (IResource) cachedEntry.getKey();
						byte[] value = (byte []) cachedEntry.getValue();
						if (value == BYTES_REMOVED)
							value = null;
						try {
							ResourcesPlugin.getWorkspace().getSynchronizer().setSyncInfo(StatusCacheManager.SVN_BC_SYNC_KEY, resource, value);
						} catch (CoreException e) {
							SVNProviderPlugin.log(SVNException.wrapException(e));
						}
						removeFromPendingCacheIfEqual((IResource) cachedEntry.getKey(), (byte []) cachedEntry.getValue());
					}
				}
			}
		}

		protected boolean isFlushFeasible()
		{
			return (pendingCacheWrites.size() > 0) && (!ResourcesPlugin.getWorkspace().isTreeLocked());
		}
		
		synchronized private Map.Entry nextFromPendingCache()
		{
			if (pendingCacheWrites.size() > 0)
			{
				return (Map.Entry) pendingCacheWrites.entrySet().iterator().next();
			}
			return null;
		}

		synchronized protected boolean pendingCacheContains(IResource resource)
		{
			return ((pendingCacheWrites.size() > 0) && (pendingCacheWrites.containsKey(resource)));
		}
		
		synchronized protected byte[] readFromPendingCache(IResource resource)
		{
			return (byte[]) pendingCacheWrites.get(resource);
		}

		synchronized protected void writeToPendingCache(IResource resource, byte[] syncBytes)
		{
			pendingCacheWrites.put(resource, syncBytes);
		}

		synchronized protected void removeFromPendingCache(IResource resource)
		{
			pendingCacheWrites.remove(resource);
		}

		/**
		 * Remove the resource from cache if the cached bytes are equal to <code>syncBytes<code>
		 * This is because the caller of this method is not synchronized, so if the cache
		 * was modified, do not remove the resource ...
		 * @param resource
		 * @param syncBytes
		 * @return true when the status was equal and removed from pendingCache  
		 */
		synchronized protected boolean removeFromPendingCacheIfEqual(IResource resource, byte[] syncBytes)
		{
			byte[] old = (byte[]) pendingCacheWrites.get(resource);
			if (equals(old, syncBytes))
			{
				pendingCacheWrites.remove(resource);
				return true;
			}
			return false;
		}

		synchronized protected void removeRecursiveFromPendingCache(IResource resource)
		{
			IPath fullPath = resource.getFullPath();
			for (Iterator iter = pendingCacheWrites.keySet().iterator(); iter.hasNext();) {
				if (fullPath.isPrefixOf(((IResource) iter.next()).getFullPath())) {
					iter.remove();
				}
			}			
		}
		
		/**
		 * Method equals.
		 * @param syncBytes
		 * @param oldBytes
		 * @return boolean
		 */
		protected static boolean equals(byte[] syncBytes, byte[] oldBytes) {
			if (syncBytes == null || oldBytes == null) return syncBytes == oldBytes;
			if (syncBytes.length != oldBytes.length) return false;
			for (int i = 0; i < oldBytes.length; i++) {
				if (oldBytes[i] != syncBytes[i]) { 
					return false;
				}
			}
			return true;
		}	
	}
}