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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.client.NotificationListener;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * This class manages a SVN repository location.
 * 
 * It manages its user info using the plugged in IUserAuthenticator
 * (unless a username and password are provided as part of the creation
 * string, in which case, no authenticator is used).
 * 
 * Instances must be disposed of when no longer needed in order to 
 * notify the authenticator so cached properties can be cleared
 * 
 */
public class SVNRepositoryLocation
	implements ISVNRepositoryLocation, IUserInfo, IAdaptable {

	private String user;
	private String password;
	private SVNUrl url;
	private RemoteFolder rootFolder;
	// the folder corresponding to this repository location

	// fields needed for caching the password
	public static final String INFO_PASSWORD = "org.eclipse.team.cvs.core.password"; //$NON-NLS-1$ 
	public static final String INFO_USERNAME = "org.eclipse.team.cvs.core.username"; //$NON-NLS-1$ 
	public static final String AUTH_SCHEME = ""; //$NON-NLS-1$ 
	public static final URL FAKE_URL;

	public static final String USER_VARIABLE = "{user}"; //$NON-NLS-1$
	public static final String PASSWORD_VARIABLE = "{password}"; //$NON-NLS-1$
	public static final String HOST_VARIABLE = "{host}"; //$NON-NLS-1$
	public static final String PORT_VARIABLE = "{port}"; //$NON-NLS-1$

	//	private ISVNClientAdapter svnClient; 

	static {
		URL temp = null;
		try {
			temp = new URL("http://org.tigris.subversion.subclipse.core"); //$NON-NLS-1$ 
		} catch (MalformedURLException e) {
		}
		// The protection space is defined by this url and realm (AUTH_SCHEME)
		FAKE_URL = temp;
	}

	/*
	 * Create a SVNRepositoryLocation from its composite parts.
	 */
	private SVNRepositoryLocation(String user, String password, SVNUrl url) {
		this.user = user;
		this.password = password;
		this.url = url;

		rootFolder = new RemoteFolder(this, url, SVNRevision.HEAD);
	}

	/*
	 * Dispose of the receiver by clearing any cached authorization information.
	 * This method should only be invoked when the corresponding adapter is shut
	 * down or a connection is being validated.
	 */
	public void dispose() throws SVNException {
		try {
			Platform.flushAuthorizationInfo(
				FAKE_URL,
				getLocation(),
				AUTH_SCHEME);
		} catch (CoreException e) {
			// We should probably wrap the CoreException here!
			SVNProviderPlugin.log(e.getStatus());
			throw new SVNException(IStatus.ERROR, IStatus.ERROR, Policy.bind("SVNRepositoryLocation.errorFlushing", getLocation()), e); //$NON-NLS-1$ 
		}
	}

	/*
	 * @see ISVNRepositoryLocation#getUrl()
	 */
	public SVNUrl getUrl() {
		return url;
	}

	/*
	 * @see IRepositoryLocation#getLocation()
	 */
	public String getLocation() {
		return getUrl().toString();
	}

	public ISVNRemoteFolder getRootFolder() {
		//        // refresh it so that members don't return always the same remote resources ...
		//        rootFolder.refresh();
		return rootFolder;
	}

	public void refreshRootFolder() {
		rootFolder.refresh();
	}

	/*
	 * @see ISVNRepositoryLocation#members(IProgressMonitor)
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress)
		throws SVNException {
		try {
			ISVNRemoteResource[] resources =
				(ISVNRemoteResource[]) getRootFolder().members(progress);
			return resources;
		} catch (TeamException e) {
			throw new SVNException(e.getStatus());
		}
	}

	/*
	 * @see ISVNRepositoryLocation#getRemoteFolder(String)
	 */
	public ISVNRemoteFolder getRemoteFolder(String remotePath) {
		try {
			return new RemoteFolder(
				this,
				new SVNUrl(Util.appendPath(getUrl().toString(), remotePath)),
				SVNRevision.HEAD);
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	public ISVNRemoteFile getRemoteFile(SVNUrl url) throws SVNException{
		ISVNClientAdapter svnClient = getSVNClient();
		ISVNDirEntry[] dirEntry = null;
		try {
			dirEntry = svnClient.getList(url, SVNRevision.HEAD, false);
		} catch (SVNClientException e) {
			throw new SVNException(
				"Can't get latest remote resource for "
					+ url);
		}

		if (dirEntry.length == 0)
			return null; // no remote file
		else {
			return new RemoteFile(null, // we don't know its parent
			this,
				url,
				SVNRevision.HEAD,
				dirEntry[0].getHasProps(),
				dirEntry[0].getLastChangedRevision(),
				dirEntry[0].getLastChangedDate(),
				dirEntry[0].getLastCommitAuthor());
		}		
	}

	public ISVNRemoteFile getRemoteFile(String remotePath) throws SVNException{
		SVNUrl url;
		try {
			url = new SVNUrl(Util.appendPath(getUrl().toString(), remotePath));
		} catch (MalformedURLException e1) {
			throw new SVNException(
				"Can't get latest remote resource for "
					+ remotePath.toString());
		}
		return getRemoteFile(url);
	}

/*
 * @see ISVNRepositoryLocation#getUsername()
 * @see IUserInfo#getUsername()
 */
public String getUsername() {
	if (user == null) {
		retrieveUsernamePassword();
	}
	return user == null ? "" : user; //$NON-NLS-1$
}

private String getPassword() {
	// user can be null but not password
	if (user == null) {
		retrieveUsernamePassword();
	}
	return password; //$NON-NLS-1$
}

/**
 * get the svn client corresponding to the repository
 */
public ISVNClientAdapter getSVNClient() {
	//		if (svnClient != null)
	//			return svnClient;
	ISVNClientAdapter svnClient =
		SVNProviderPlugin.getPlugin().createSVNClient();

	svnClient.addNotifyListener(NotificationListener.getInstance());

	svnClient.setUsername(getUsername());
	String passWord = getPassword();
	if (password != null)
		svnClient.setPassword(password);
	return svnClient;
}

/*
 * Implementation of inherited toString()
 */
public String toString() {
	return getLocation();
}

public boolean equals(Object o) {
	if (!(o instanceof SVNRepositoryLocation))
		return false;
	return getLocation().equals(((SVNRepositoryLocation) o).getLocation());
}

public int hashCode() {
	return getLocation().hashCode();
}

/*
 * Retrieves the cached user & password from the keyring. 
 */
private void retrieveUsernamePassword() {
	Map map =
		Platform.getAuthorizationInfo(FAKE_URL, getLocation(), AUTH_SCHEME);
	if (map != null) {
		String username = (String) map.get(INFO_USERNAME);
		if (username != null)
			setUsername(username);
		String password = (String) map.get(INFO_PASSWORD);
		if (password != null) {
			setPassword(password);
		}
	}
}

/*
 * @see IUserInfo#setPassword(String)
 */
public void setPassword(String password) {
	this.password = password;
}

/*
 * @see IUserInfo#setUsername(String)
 */
public void setUsername(String user) {
	this.user = user;
}

/**
 * add user and password to the keyring 
 */
public void updateCache() throws SVNException {
	// put the password into the Platform map
	Map map =
		Platform.getAuthorizationInfo(FAKE_URL, getLocation(), AUTH_SCHEME);
	if (map == null) {
		map = new java.util.HashMap(10);
	}
	if (user != null)
		map.put(INFO_USERNAME, user);
	if (password != null)
		map.put(INFO_PASSWORD, password);
	try {
		Platform.addAuthorizationInfo(
			FAKE_URL,
			getLocation(),
			AUTH_SCHEME,
			map);
	} catch (CoreException e) {
		// We should probably wrap the CoreException here!
		SVNProviderPlugin.log(e.getStatus());
		throw new SVNException(IStatus.ERROR, IStatus.ERROR, Policy.bind("SVNRepositoryLocation.errorCaching", getLocation()), e); //$NON-NLS-1$ 
	}

	password = null;
	// Ensure that the receiver is known by the SVN provider
	SVNProviderPlugin.getPlugin().getRepository(getLocation());
}

/*
 * Validate that the receiver contains valid information for
 * making a connection. If the receiver contains valid
 * information, the method returns. Otherwise, an exception
 * indicating the problem is throw.
 */
public void validateConnection(IProgressMonitor monitor) throws SVNException {
	ISVNClientAdapter svnClient = getSVNClient();
	try {
		// we try to get the list of directories and files using the connection
		svnClient.getList(getUrl(), SVNRevision.HEAD, false);
	} catch (SVNClientException e) {
		// If the validation failed, dispose of any cached info
		dispose();
		throw SVNException.wrapException(e);
	}
}

/*
 * Create a repository location instance from the given properties.
 * The supported properties are:
 *   user The username for the connection (optional)
 *   password The password used for the connection (optional)
 *   url The url where the repository resides
 */
public static SVNRepositoryLocation fromProperties(Properties configuration)
	throws SVNException {
	// We build a string to allow validation of the components that are provided to us

	String user = configuration.getProperty("user"); //$NON-NLS-1$ 
	if ((user == null) || (user.length() == 0))
		user = null;
	String password = configuration.getProperty("password"); //$NON-NLS-1$ 
	if (user == null)
		password = null;
	String url = configuration.getProperty("url"); //$NON-NLS-1$ 
	if (url == null)
		throw new SVNException(new Status(IStatus.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNRepositoryLocation.hostRequired"), null)); //$NON-NLS-1$ 

	SVNUrl urlURL = null;
	try {
		urlURL = new SVNUrl(url);
	} catch (MalformedURLException e) {
		throw new SVNException(e.getMessage());
	}

	return new SVNRepositoryLocation(user, password, urlURL);
}

/*
 * Parse a location string and return a SVNRepositoryLocation.
 * 
 * On failure, the status of the exception will be a MultiStatus
 * that includes the original parsing error and a general status
 * displaying the passed location and proper form. This form is
 * better for logging, etc.
 */
public static SVNRepositoryLocation fromString(String location)
	throws SVNException {
	try {
		return fromString(location, false);
	} catch (SVNException e) {
		// Parsing failed. Include a status that
		// shows the passed location and the proper form
		MultiStatus error = new MultiStatus(SVNProviderPlugin.ID, SVNStatus.ERROR, Policy.bind("SVNRepositoryLocation.invalidFormat", new Object[] { location }), null); //$NON-NLS-1$ 
		error.merge(new SVNStatus(SVNStatus.ERROR, Policy.bind("SVNRepositoryLocation.locationForm"))); //$NON-NLS-1$ 
		error.merge(e.getStatus());
		throw new SVNException(error);
	}
}

/*
 * Parse a location string and return a SVNRepositoryLocation.
 * 
 * The location is an url
 */
public static SVNRepositoryLocation fromString(
	String location,
	boolean validateOnly)
	throws SVNException {

	String partId = null;
	try {
		String user = null;
		String password = null;
		SVNUrl url = new SVNUrl(location);

		if (validateOnly)
			throw new SVNException(new SVNStatus(SVNStatus.OK, Policy.bind("ok"))); //$NON-NLS-1$ 

		return new SVNRepositoryLocation(user, password, url);
	} catch (MalformedURLException e) {
		throw new SVNException(Policy.bind(partId));
	} catch (IndexOutOfBoundsException e) {
		// We'll get here if anything funny happened while extracting substrings
		throw new SVNException(Policy.bind(partId));
	} catch (NumberFormatException e) {
		// We'll get here if we couldn't parse a number
		throw new SVNException(Policy.bind(partId));
	}
}

//	public static IUserAuthenticator getAuthenticator() {
//		if (authenticator == null) {
//			authenticator = getPluggedInAuthenticator();
//		}
//		return authenticator;
//	}

/*
 * Return the connection method registered for the given name or null if none
 * are registered
 */
//	private static IConnectionMethod getPluggedInConnectionMethod(String methodName) {
//		IConnectionMethod[] methods = getPluggedInConnectionMethods();
//		for(int i=0; i<methods.length; i++) {
//			if(methodName.equals(methods[i].getName()))
//				return methods[i];
//		}
//		return null;		
//	}

//	private static IUserAuthenticator getPluggedInAuthenticator() {
//		IExtension[] extensions = Platform.getPluginRegistry().getExtensionPoint(CVSProviderPlugin.ID, CVSProviderPlugin.PT_AUTHENTICATOR).getExtensions();
//		if (extensions.length == 0)
//			return null;
//		IExtension extension = extensions[0];
//		IConfigurationElement[] configs = extension.getConfigurationElements();
//		if (configs.length == 0) {
//			CVSProviderPlugin.log(new Status(IStatus.ERROR, CVSProviderPlugin.ID, 0, Policy.bind("CVSAdapter.noConfigurationElement", new Object[] {extension.getUniqueIdentifier()}), null));//$NON-NLS-1$ 
//			return null;
//		}
//		try {
//			IConfigurationElement config = configs[0];
//			return (IUserAuthenticator) config.createExecutableExtension("run");//$NON-NLS-1$ 
//		} catch (CoreException ex) {
//			CVSProviderPlugin.log(new Status(IStatus.ERROR, CVSProviderPlugin.ID, 0, Policy.bind("CVSAdapter.unableToInstantiate", new Object[] {extension.getUniqueIdentifier()}), ex));//$NON-NLS-1$ 
//			return null;
//		}
//	}

public Object getAdapter(Class adapter) {
	if (adapter == ISVNRemoteFolder.class)
		return rootFolder;
	else
		return Platform.getAdapterManager().getAdapter(this, adapter);
}

 /*
 *  this should be made more robust --mml 11/27/03
 * @see org.tigris.subversion.subclipse.core.ISVNRepositoryLocation#pathExists()
 */
public boolean pathExists(){
	ISVNClientAdapter svnClient = getSVNClient();
	try{
		svnClient.getList(getUrl(), SVNRevision.HEAD, false);
	}catch(SVNClientException e){
		return false;
	}
	return true;
}

}
