/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.repository;

import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

/**
 * Listener for repositories. events fired when repository added, removed or changed 
 */
public interface IRepositoryListener {
	public void repositoryAdded(ISVNRepositoryLocation root);
    public void repositoryModified(ISVNRepositoryLocation root);
	public void repositoryRemoved(ISVNRepositoryLocation root);
	public void repositoriesChanged(ISVNRepositoryLocation[] roots);
    public void remoteResourceDeleted(ISVNRemoteResource resource);
    public void remoteResourceCreated(ISVNRemoteFolder parent,String resourceName);
    public void remoteResourceCopied(ISVNRemoteResource source,ISVNRemoteFolder destination);
    public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder,String destinationResourceName);
}

