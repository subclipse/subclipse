/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
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

