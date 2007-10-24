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
package org.tigris.subversion.subclipse.core.status;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

/**
 * When the status of a resource is asked, we don't update a resource at once.
 * We use a strategy to get the status of several resources at once.
 * There are more than one strategy. All strategies inherit from this class
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public abstract class StatusUpdateStrategy {
	protected IStatusCache statusCache;
    	
	public StatusUpdateStrategy(IStatusCache statusCache)
	{
		super();
		setStatusCache(statusCache);
	}
	
	/**
	 * @param treeCacheRoot The treeCacheRoot to set.
	 */
	public void setStatusCache(IStatusCache statusCache) {
		this.statusCache = statusCache;
	}

    /**
     * update the status of the given resource and possibly to other resources 
     * as well depending on the strategy
     * @param resource
     * @throws SVNException
     */
    abstract ISVNStatus[] statusesToUpdate(IResource resource) throws SVNException;

}
