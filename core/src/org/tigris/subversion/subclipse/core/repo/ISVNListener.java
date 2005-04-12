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
package org.tigris.subversion.subclipse.core.repo;

import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;



public interface ISVNListener {
	
    /**
     * called when a new repository is added to the list of repositories 
     */
    public void repositoryAdded(ISVNRepositoryLocation root);
    
    /**
     * called when a repository location has been modified (label, username, password)
     * @param root
     */
    public void repositoryModified(ISVNRepositoryLocation root);
    
    /**
     * called when a repository is removed from the list of repositories 
     */
	public void repositoryRemoved(ISVNRepositoryLocation root);
    
    /**
     * called when a remote resource has been deleted
     */
    public void remoteResourceDeleted(ISVNRemoteResource resource);
    
    /**
     * called when a remote resource has been created
     */
    public void remoteResourceCreated(ISVNRemoteFolder parent,String resourceName);

    /**
     * called when a remote resource has been copied 
     */    
    public void remoteResourceCopied(ISVNRemoteResource source, ISVNRemoteFolder destination);

    /**
     * called when a remote resource has been moved 
     */
    public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder, String destinationResourceName);
        
}

