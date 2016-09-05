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

 
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNUrl;

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
	 * Returns the host where the repository is located
	 */
	public SVNUrl getUrl();
	

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
	
	public ISVNRemoteFile getRemoteFile(String remotePath)  throws SVNException;
	
	public ISVNRemoteFile getRemoteFile(SVNUrl url)  throws SVNException;	

	/**
	 * Return the conection timeout value in milliseconds.
	 * A value of 0 means there is no timeout value.
	 */
//	public int getTimeout();
	
	/**
	 * Return the username 
	 */
	public String getUsername();
	
    /**
     * Exists for pre-1.0 compatibility.  It is not expected
     * that users of this class will use this method
     * @deprecated
     */
    public void setUsername(String username);
    
	
    /**
     * Exists for pre-1.0 compatibility.  It is not expected
     * that users of this class will use this method
     * @deprecated
     */
    public void setPassword(String password);
    
    /**
     * add user and password to the keyring 
     */
    public void updateCache() throws SVNException;
    
    
	public ISVNClientAdapter getSVNClient() throws SVNException;
	
	public void returnSVNClient(ISVNClientAdapter client);
	
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
     * returns the label (friendly name for the repository location) or null if no label
     */
    public String getLabel();
    
    public void setLabel(String label);
    
	/**
	 * Validate that the receiver can be used to connect to a repository.
	 * An exception is thrown if connection fails
	 * 
	 * @param monitor the progress monitor used while validating
	 */
	public void validateConnection(IProgressMonitor monitor) throws SVNException;
	
		
	/**
	 * Verify that said location contains said path.
	 * @throws SVNException
	 *
	 */
	public boolean pathExists() throws SVNException;

    /**
     * get the repository root url
     * @return
     */
    public SVNUrl getRepositoryRoot();
    
    /**
     * set the repository root url
     * @param url
     */
    public void setRepositoryRoot(SVNUrl url);
    
}

