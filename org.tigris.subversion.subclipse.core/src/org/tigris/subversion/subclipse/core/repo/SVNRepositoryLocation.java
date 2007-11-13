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
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.team.core.TeamException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
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
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * This class manages a SVN repository location.
 * <br>
 * After modifying a SVNRepositoryLocation (label, username, password),
 * add it to repositories using {@link SVNRepositories#addOrUpdateRepository(ISVNRepositoryLocation)}
 */
public class SVNRepositoryLocation
	implements ISVNRepositoryLocation, IUserInfo, IAdaptable {

	/**
	 * The name of the preferences node in the CVS preferences that contains
	 * the known repositories as its children.
	 */
	public static final String PREF_REPOSITORIES_NODE = "repositories"; //$NON-NLS-1$
	
	/*
	 * The name of the node in the default scope that has the default settings
	 * for a repository.
	 */
	private static final String DEFAULT_REPOSITORY_SETTINGS_NODE = "default_repository_settings"; //$NON-NLS-1$

	// Preference keys used to persist the state of the location
	public static final String PREF_LOCATION = "location"; //$NON-NLS-1$
	public static final String PREF_SERVER_ENCODING = "encoding"; //$NON-NLS-1$

	// friendly name of the location
	private String label = null; 
	
    private String user;
	private String password;
	
    // url of this location
    private SVNUrl url;
	
    // url of the root repository
    private SVNUrl repositoryRootUrl;
    
	// the folder corresponding to this repository location
    private RemoteFolder rootFolder;
	

	// fields needed for caching the password
	public static final String INFO_PASSWORD = "org.tigris.subversion.subclipse.core.password"; //$NON-NLS-1$ 
	public static final String INFO_USERNAME = "org.tigris.subversion.subclipse.core.username"; //$NON-NLS-1$ 
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

	/**
	 * Return the preferences node whose child nodes are the know repositories
	 * @return a preferences node
	 */
	public static Preferences getParentPreferences() {
		return SVNProviderPlugin.getPlugin().getInstancePreferences().node(PREF_REPOSITORIES_NODE);
	}
	
	/**
	 * Return a preferences node that contains suitabel defaults for a
	 * repository location.
	 * @return  a preferences node
	 */
	public static Preferences getDefaultPreferences() {
		Preferences defaults = new DefaultScope().getNode(SVNProviderPlugin.ID).node(DEFAULT_REPOSITORY_SETTINGS_NODE);
		defaults.put(PREF_SERVER_ENCODING, getDefaultEncoding());
		return defaults;
	}
	
	private static String getDefaultEncoding() {
		return System.getProperty("file.encoding", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
	}	
	/*
	 * Create a SVNRepositoryLocation from its composite parts.
	 */
	private SVNRepositoryLocation(String user, String password, SVNUrl url, SVNUrl repositoryRootUrl) {
		this.user = user;
		this.password = password;
		this.url = url;
        this.repositoryRootUrl = repositoryRootUrl; 

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
				ensurePreferencesStored();
		} catch (CoreException e) {
			// We should probably wrap the CoreException here!
			SVNProviderPlugin.log(e.getStatus());
			throw new SVNException(IStatus.ERROR, IStatus.ERROR, Policy.bind("SVNRepositoryLocation.errorFlushing", getLocation()), e); //$NON-NLS-1$ 
		}
		// remove repo location from preferences
		try {
			if (hasPreferences()) {
				internalGetPreferences().removeNode();
				getParentPreferences().flush();
			}
		} catch (BackingStoreException e) {
			SVNProviderPlugin.log(SVNException.wrapException(e));
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
				getRootFolder().members(progress);
			return resources;
		} catch (TeamException e) {
			throw new SVNException(e.getStatus());
		}
	}


	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRepositoryLocation#getRemoteFolder(java.lang.String)
	 */
	public ISVNRemoteFolder getRemoteFolder(String remotePath) {
		return new RemoteFolder(
				this,
				getUrl().appendPath(remotePath),
				SVNRevision.HEAD);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRepositoryLocation#getRemoteFile(org.tigris.subversion.svnclientadapter.SVNUrl)
	 */
	public ISVNRemoteFile getRemoteFile(SVNUrl url) throws SVNException{
		ISVNClientAdapter svnClient = getSVNClient();
		ISVNInfo info = null;
		try {
			if (this.getRepositoryRoot().equals(url))
			    return new RemoteFile(this, url, SVNRevision.HEAD);
			else
			    info = svnClient.getInfo(url, SVNRevision.HEAD, SVNRevision.HEAD);
		} catch (SVNClientException e) {
			throw new SVNException(
				"Can't get latest remote resource for "
					+ url);
		}

		if (info == null)
			return null; // no remote file
		else {
			return new RemoteFile(null, // we don't know its parent
			this,
				url,
				SVNRevision.HEAD,
				info.getLastChangedRevision(),
				info.getLastChangedDate(),
				info.getLastCommitAuthor());
		}		
	}

	public ISVNRemoteFile getRemoteFile(String remotePath) throws SVNException{
		return getRemoteFile(getUrl().appendPath(remotePath));
	}

    /*
     * @see ISVNRepositoryLocation#getUsername()
     * @see IUserInfo#getUsername()
     */
    public String getUsername() {
    	if (user == null) {
    		retrieveUsername();
    	}
    	return user == null ? "" : user; //$NON-NLS-1$
    }

    /**
     * get the svn client corresponding to the repository
     * @throws SVNException
     */
    public ISVNClientAdapter getSVNClient() throws SVNException {
    	ISVNClientAdapter svnClient =
    		SVNProviderPlugin.getPlugin().createSVNClient();
    
    	svnClient.addNotifyListener(NotificationListener.getInstance());
    
    	svnClient.setUsername(getUsername());
        String password = getPassword();
    	if (password != null)
    		svnClient.setPassword(password);
    	return svnClient;
    }

    /*
     * Implementation of inherited toString()
     */
    public String toString() {
    	if (getLabel() != null) {
    		return getLabel();
        } else
        {
        	return getLocation();
        }
    }
    
    public boolean equals(Object o) {
    	if (!(o instanceof SVNRepositoryLocation))
    		return false;
    	return getLocation().equals(((SVNRepositoryLocation) o).getLocation());
    }
    
    public int hashCode() {
    	return getLocation().hashCode();
    }
    
    /**
     * Retrieves the cached username from the keyring. 
     */
    private void retrieveUsername() {
    	Map map =
    		Platform.getAuthorizationInfo(FAKE_URL, getLocation(), AUTH_SCHEME);
    	if (map != null) {
    		String username = (String) map.get(INFO_USERNAME);
    		if (username != null)
    			setUsername(username);
    	}
    }
    
    /**
     * Retrieves the cached password
     * @return
     */
    private String retrievePassword() {
        Map map =
            Platform.getAuthorizationInfo(FAKE_URL, getLocation(), AUTH_SCHEME);
        if (map != null) {
            String password = (String) map.get(INFO_PASSWORD);
            if (password != null) {
                return password;
            }        
        }
        return null;
    }
    
    /**
     * get the password
     * @return
     */
    private String getPassword() {
    	if (password != null) {
    		return password;
        } else {
        	return retrievePassword();
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
        // If the cache was updated, null the password field
        // so we will obtain the password from the cache when needed
    	password = null;
    	// Ensure that the receiver is known by the SVN provider
    	SVNProviderPlugin.getPlugin().getRepository(getLocation());
    	// Ensure location is stored in plugin preferences
    	ensurePreferencesStored();
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
    		ISVNInfo info = svnClient.getInfo(getUrl());
   		    repositoryRootUrl = info.getRepository();
    	} catch (SVNClientException e) {
    		// If the validation failed, dispose of any cached info
    		dispose();
    		throw SVNException.wrapException(e);
    	}
    }

    /*
     *  this should be made more robust --mml 11/27/03
     * @see org.tigris.subversion.subclipse.core.ISVNRepositoryLocation#pathExists()
     */
    public boolean pathExists() throws SVNException{
    	ISVNClientAdapter svnClient = getSVNClient();
    	try{
    		svnClient.getList(getUrl(), SVNRevision.HEAD, false);
    	}catch(SVNClientException e){
    		return false;
    	}
    	return true;
    }

    /*
     * Create a repository location instance from the given properties.
     * The supported properties are:
     *   user The username for the connection (optional)
     *   password The password used for the connection (optional)
     *   url The url where the repository resides
     *   rootUrl The repository root url
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
        String rootUrl = configuration.getProperty("rootUrl"); //$NON-NLS-1$
        if ((rootUrl == null) || (rootUrl.length() == 0))
            rootUrl = null;
    	String url = configuration.getProperty("url"); //$NON-NLS-1$ 
    	if (url == null)
    		throw new SVNException(new Status(IStatus.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNRepositoryLocation.hostRequired"), null)); //$NON-NLS-1$ 
    
    	SVNUrl urlURL = null;
    	try {
    		urlURL = new SVNUrl(url);
    	} catch (MalformedURLException e) {
    		throw new SVNException(e.getMessage());
    	}
    
        SVNUrl rootUrlURL = null;
        if (rootUrl != null) {
            try {
                rootUrlURL = new SVNUrl(rootUrl);
            } catch (MalformedURLException e) {
                throw new SVNException(e.getMessage());
            }
        }
        
    	return new SVNRepositoryLocation(user, password, urlURL, rootUrlURL);
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
            SVNUrl rootUrl = null;
    		SVNUrl url = new SVNUrl(location);
    
    		if (validateOnly)
    			throw new SVNException(new SVNStatus(SVNStatus.OK, Policy.bind("ok"))); //$NON-NLS-1$ 
    
    		return new SVNRepositoryLocation(user, password, url, rootUrl);
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

    /*
     * Parse a location string and return a SVNRepositoryLocation.
     * If useRootUrl is true, use the repository root URL.
     */    
    public static SVNRepositoryLocation fromString(
    		String location,
    		boolean validateOnly,
    		boolean useRootUrl) throws SVNException {
    	if (!useRootUrl) return fromString(location, validateOnly);
    	ISVNClientAdapter svnClient =
    		SVNProviderPlugin.getPlugin().createSVNClient();
    	try {
	    	SVNUrl url = new SVNUrl(location);
	    	ISVNInfo info = svnClient.getInfo(url);
	    	SVNUrl rootUrl = info.getRepository();
	    	return fromString(rootUrl.toString());
    	} catch (MalformedURLException e) {
    		throw SVNException.wrapException(e);
    	} catch (SVNClientException e) {
    		throw SVNException.wrapException(e);
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

    /**
     * get the url of the repository root <br>
     * Ex : if url is http://svn.collab.net/viewcvs/svn/trunk/subversion/, the
     * repository root is http://svn.collab.net/viewcvs/svn
     * @return
     */
    public SVNUrl getRepositoryRoot() {
        // for now, we can't get it using svn, so user must give it
        // if the user did not provide a value, we will just return
        // the repository URL.
        if (repositoryRootUrl == null)
            return getUrl();
        else
            return repositoryRootUrl;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNRepositoryLocation#setRepositoryRoot(org.tigris.subversion.svnclientadapter.SVNUrl)
     */
    public void setRepositoryRoot(SVNUrl url) {
        repositoryRootUrl = url;
    }

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * @param label The label to set.
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	/*
	 * Return the preferences node for this repository
	 */
	public Preferences getPreferences() {
		if (!hasPreferences()) {
			ensurePreferencesStored();
		}
		return internalGetPreferences();
	}
	
	private Preferences internalGetPreferences() {
		return getParentPreferences().node(getPreferenceName());
	}
	
	private boolean hasPreferences() {
		try {
			return getParentPreferences().nodeExists(getPreferenceName());
		} catch (Exception e) {
			// FIXME: commented 2 lines below is how CVS did it. Did i do it right?
			//CVSProviderPlugin.log(IStatus.ERROR, NLS.bind(CVSMessages.CVSRepositoryLocation_74, new String[] { getLocation(true) }), e); 
			//return false;
			SVNProviderPlugin.log(SVNException.wrapException(e));
			return false;
		}
	}
	
	/**
	 * Return a unique name that identifies this location but
	 * does not contain any slashes (/). Also, do not use ':'.
	 * Although a valid path character, the initial core implementation
	 * didn't handle it well.
	 */
	private String getPreferenceName() {
		return getLocation().replace('/', '%').replace(':', '%');
	}

	public void storePreferences() {
		Preferences prefs = internalGetPreferences();
		// Must store at least one preference in the node
		prefs.put(PREF_LOCATION, getLocation());
		flushPreferences();
	}
	
	private void flushPreferences() {
		try {
			internalGetPreferences().flush();
		} catch (BackingStoreException e) {
			SVNProviderPlugin.log(SVNException.wrapException(e));
		}
	}
	public void setUrl(SVNUrl url) {
		this.url = url;
	}
	private void ensurePreferencesStored() {
		if (!hasPreferences()) {
			storePreferences();
		}
	}

}
