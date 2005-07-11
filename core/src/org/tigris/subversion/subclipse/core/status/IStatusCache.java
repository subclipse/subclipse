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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;

/**
 * Cache for storing and retrieving local status info
 * 
 */
public interface IStatusCache {

    /**
     * Get the status of the given resource (which does not need to exist)
     * @param resource
     * @return LocalResourceStatus
     */
    LocalResourceStatus getStatus(IResource resource);

    /**
     * Add a status for its resource (which does not need to exist)
     * @param status - can be null
     * @return resource for which the status was cached
     */
    IResource addStatus(LocalResourceStatus status);

    /**
     * Remove status of the given resource from the cache
     * @param resource
     * @return
     */
    IResource removeStatus(IResource resource);
    
    /**
     * Purge (remove) the status information from the cache.
     * @param root
     * @param deep
     * @throws SVNException
     */
	void purgeCache(IContainer root, boolean deep) throws SVNException;

}