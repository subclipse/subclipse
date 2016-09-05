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
package org.tigris.subversion.subclipse.core;

import org.eclipse.core.runtime.IProgressMonitor;

 

 /**
  * This interface represents a remote folder in a repository. It provides
  * access to the members (remote files and folders) of a remote folder
  * 
  * Clients are not expected to implement this interface.
  */
public interface ISVNRemoteFolder extends ISVNRemoteResource, ISVNFolder{

    /**
     * Create a new remote folder 
     * @param folderName a folder name
     * @param message a commit message
     * @param monitor a progress monitor
     * @throws SVNException
     */
    void createRemoteFolder(String folderName, String message, IProgressMonitor monitor) throws SVNException;	
	
    /**
     * Empty the cache of children
     */
    void refresh();
}
