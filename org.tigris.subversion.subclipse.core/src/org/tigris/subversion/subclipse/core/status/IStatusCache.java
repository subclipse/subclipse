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
	 * @param resource
	 * @return true when the resource's status is present in cache
	 */
	boolean hasCachedStatus(IResource resource);
	
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
    IResource addStatus(IResource resource, LocalResourceStatus status);

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

	/**
	 * Flush (pending) statuses which were not saved yet due to e.g. locked workspace
	 */
	void flushPendingStatuses();
}