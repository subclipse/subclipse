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
import org.eclipse.core.runtime.IPath;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;

/**
 * Cache for storing and retrieving local status info
 * 
 */
public interface IStatusCache {

    /**
     * get the status of the given resource (which does not need to exist)
     * @param resource
     * @return LocalResourceStatus
     */
    LocalResourceStatus getStatus(IResource resource);

    /**
     * add a status for the given resource (which does not need to exist)
     * @param resource
     * @param status - can be null
     */
    void addStatus(IResource resource, LocalResourceStatus status);

    /**
     * add a status for the reasource of  the given relative path
     * @param path
     * @param status - can be null
     */
    void addStatus(IPath path, LocalResourceStatus status);

	/**
	 * Ensure that base(pristine) copy of resource syncInfo is present in syncInfo of ResourceInfo of the resource.
	 * 
	 * @param resource IResource of status is determined
	 * @throws SVNException
	 * @deprecated should be removed when StatusCacheComposite will be definitely replaced by SynchronizerSyncInfoCache
	 */
	void ensureBaseStatusInfo(IResource resource) throws SVNException;

	/**
	 * Ensure that base(pristine) copy of resource syncInfo is present in syncInfo of ResourceInfo of the resource.
	 * 
	 * @param resource IResource of status is determined
	 * @param depth
	 * @throws SVNException
	 * @deprecated should be removed when StatusCacheComposite will be definitely replaced by SynchronizerSyncInfoCache
	 */
	void ensureBaseStatusInfo(IResource resource, int depth) throws SVNException;

}
