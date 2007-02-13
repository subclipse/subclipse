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

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.svnclientadapter.SVNUrl;





/**
 * The SVN analog of file system files and directories. These are handles to
 * state maintained by a SVN client. That is, the SVN resource does not 
 * actually contain data but rather represents SVN state and behavior. You are
 * free to manipulate handles for SVN resources that do not exist but be aware
 * that some methods require that an actual resource be available.
 * <p>
 * The SVN client has been designed to work on these handles uniquely. As such, the
 * handle could be to a remote resource or a local resource and the client could
 * perform SVN operations ignoring the actual location of the resources.</p>
 * 
 * @see ISVNFolder
 * @see ISVNFile
 */
public interface ISVNResource {
	
	/**
	 * Answers the name of the resource.
	 * 
	 * @return the name of the resource this handle represents. It can never
	 * be <code>null</code>.
	 */
	public String getName();
	
	/**
	 * Answers if the handle is a file or a folder handle.
	 * 
	 * @return <code>true</code> if this is a folder handle and <code>false</code> if
	 * it is a file handle.
	 */
	public boolean isFolder();

    /**
     * @return the repository location
     * @throws SVNException
     */	
	public ISVNRepositoryLocation getRepository();
	
    /**
     * get the url corresponding to this resource (which needs not to exist)
     * @throws SVNException
     */
    public SVNUrl getUrl();	
    
    /**
     * Gets the underlying resource of this SVN resource if there is one
     * @return Local resource or <code>null</code> if this is a remote resource with
     * no corresponding local resource
     */
    public IResource getResource();
	
}
