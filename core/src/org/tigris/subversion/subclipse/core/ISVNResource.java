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
package org.tigris.subversion.subclipse.core;




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
	 * Answer whether the resource could be ignored.
     * Even if a resource is ignored, it can still be added to a repository, at which 
     * time it should never be ignored by the SVN client.
	 * 
	 */
	public boolean isIgnored() throws SVNException;
	
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
	public ISVNRepositoryLocation getRepository() throws SVNException;	
	
}
