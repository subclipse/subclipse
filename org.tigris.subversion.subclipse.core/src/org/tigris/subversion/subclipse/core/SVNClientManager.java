/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.clientadapter.Activator;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Handles the creation of SVNClients
 * 
 * @author Cedric Chabanois (cchab at tigris.org) 
 */
public class SVNClientManager {
	private static String DEFAULT_ADMIN_DIR = ".svn";
    private String svnClientInterface;
    private String svnAdminDir = null;
    private File configDir = null;
    private boolean fetchChangePathOnDemand = true;
    private HashMap<String, ISVNClientAdapter> cachedClients = null;
    
    public static final String UNABLE_TO_LOAD_DEFAULT_CLIENT = "Unable to load default SVN Client";
    
    public void startup(IProgressMonitor monitor) throws CoreException {
    }
    
    
	public void shutdown(IProgressMonitor monitor) throws CoreException {
	}

    /**
     * set the client interface to use
     * 
     * @param svnClientInterface
     */
    public void setSvnClientInterface(String svnClientInterface) {
    	this.svnClientInterface = svnClientInterface;
    }

    /**
     * get the current svn client interface used
     * @return
     */
    public String getSvnClientInterface() {
        return svnClientInterface;
    }    
    
    public String getSvnAdminDirectory() {
    	if (svnAdminDir == null)
    		return DEFAULT_ADMIN_DIR;
    	return svnAdminDir;
    }
    
	/**
	 * @param configDir The configDir to set.
	 */
	public void setConfigDir(File configDir) {
		this.configDir = configDir;
		if (cachedClients == null) return;
		
		// Update configDir in stored clients
		Set<String> keys = cachedClients.keySet();
		for (String key : keys) {
			ISVNClientAdapter svnClient = cachedClients.get(key);
			if (svnClient != null) {
				try {
					svnClient.setConfigDirectory(configDir);
				} catch (SVNClientException e) {
					break;
				}
			}
		}
	}
    
    /**
     * @return the cached ISVNClientAdapter for the client interface
     * @throws SVNClientException
     */
    public ISVNClientAdapter getSVNClient() throws SVNException {
    	ISVNClientAdapter svnClient = this.getAdapter(svnClientInterface);
    	if (svnClient == null) {
    		svnClient = this.getAdapter(null);
    	}
    	if (svnClient == null)
    		throw new SVNException("No client adapters available.");
    	return svnClient;
    }

	private void setupClientAdapter(ISVNClientAdapter svnClient)
			throws SVNException {
		if (configDir != null) {
    		try {
				svnClient.setConfigDirectory(configDir);
			} catch (SVNClientException e) {
	        	throw SVNException.wrapException(e);
			}
    	} 
    	if (SVNProviderPlugin.getPlugin().getSvnPromptUserPassword() != null)
    	    svnClient.addPasswordCallback(SVNProviderPlugin.getPlugin().getSvnPromptUserPassword());
    	if (svnAdminDir == null)
    		svnAdminDir = svnClient.getAdminDirectoryName();
	}
    
    private ISVNClientAdapter getAdapter(String key) throws SVNException {
    	ISVNClientAdapter client = null;
    	if (key == null) {
    		key = "default";
     	}
    	if (cachedClients != null) // See if we have cached a client
    		client = (ISVNClientAdapter) cachedClients.get(key);
    	if (client == null) {
    		if (!key.equals("default"))
    			client = Activator.getDefault().getClientAdapter(svnClientInterface);
    		
    		if (client == null)
    			client = Activator.getDefault().getAnyClientAdapter();
    		
    		if (client == null)
    			throw new SVNException(UNABLE_TO_LOAD_DEFAULT_CLIENT);
    		
    		setupClientAdapter(client);
    		if (client.isThreadsafe())
    			cacheClient(key, client);
    	}
    	return client;
    }

	/**
	 * @return Returns the fetchChangePathOnDemand.
	 */
	public boolean isFetchChangePathOnDemand() {
		return fetchChangePathOnDemand;
	}
	/**
	 * @param fetchChangePathOnDemand The fetchChangePathOnDemand to set.
	 */
	
	public void setFetchChangePathOnDemand(
			boolean fetchChangePathOnDemand) {
		this.fetchChangePathOnDemand = fetchChangePathOnDemand;
	}
	
	public void returnSVNClient(ISVNClientAdapter client) {
		if (client == null || client.isThreadsafe())
			return;
		// For non-threadsafe clients we are done with the object so 
		// let it clean up any resources it has allocated.
		client.dispose();
		client = null;
	}
	
	private void cacheClient(String key, ISVNClientAdapter client){
		if (cachedClients == null)
			cachedClients = new HashMap<String, ISVNClientAdapter>();
		cachedClients.put(key, client);
	}
	
}
