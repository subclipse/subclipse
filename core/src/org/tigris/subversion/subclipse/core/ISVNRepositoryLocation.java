/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.core;

 
import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

/**
 * This interface provides access to the specific portions of
 * the repository location string for use by connection methods
 * and the user authenticator.
 * 
 * It is not intended to implemented by clients.
 * 
 * @see IUserAuthenticator
 * @see IConnectionMethod
 */
public interface ISVNRepositoryLocation  extends IAdaptable {

	/**
	 * port value which indicates to a connection method to use the default port
	 */
	public static int USE_DEFAULT_PORT = 0;
	
	/**
	 * Return the connection method for making the connection
	 */
//	public IConnectionMethod getMethod();
	
	/**
	 * Returns the host where the repository is located
	 */
	public URL getUrl();
	

	/**
	 * Returns the string representing the receiver. This string
	 * should contain enough information to recreate the receiver.
	 */
	public String getLocation();

	/**
	 * Returns the immediate children of this location. If tag is <code>null</code> the
	 * HEAD branch is assumed.
	 * 
	 * If modules is true, then the module definitions from the CVSROOT/modules file are returned.
	 * Otherwise, the root level projects are returned.
	 * 
	 * @param tag the context in which to return the members (e.g. branch or version).
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress)  throws SVNException;
	
	/**
	 * Returns a handle to a remote file at this repository location using the given tag as the
	 * context. The corresponding remote file may not exist or may be a folder.
	 */
//	public ICVSRemoteFile getRemoteFile(String remotePath, CVSTag tag);
	
	/**
	 * Returns a handle to a remote folder at this repository location using the given tag as the
	 * context. The corresponding remote folder may not exist or may be a file.
	 */
    public ISVNRemoteFolder getRemoteFolder(String remotePath);
	
	/**
	 * Return the conection timeout value in milliseconds.
	 * A value of 0 means there is no timeout value.
	 */
//	public int getTimeout();
	
	/**
	 * Return the username 
	 */
	public String getUsername();
	
	public SVNClientAdapter getSVNClient();	
	
    public ISVNRemoteFolder getRootFolder();
    
    public void refreshRootFolder();
    
	/**
	 * Returns the user information for the location.
	 */
//	public IUserInfo getUserInfo(boolean allowModificationOfUsername);	
	
	/**
	 * Flush any cahced user information related to the repository location
	 */
//	public void flushUserInfo() throws CVSException;
	
	/**
	 * Validate that the receiver can be used to connect to a repository.
	 * An exception is thrown if connection fails
	 * 
	 * @param monitor the progress monitor used while validating
	 */
	public void validateConnection(IProgressMonitor monitor) throws SVNException;
}

