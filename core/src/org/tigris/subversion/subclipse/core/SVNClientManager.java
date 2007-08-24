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
    private String svnClientInterface;  
    private File configDir = null;
    private boolean fetchChangePathOnDemand = true;
    private HashMap clients = new HashMap();
    
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
    	
        // Initialize the admin directory name -- fixes a crash scenario with JavaHL
        // Since the method being called internally caches the result it
        // avoids a race condition later on, when someone runs an action like
        // checkout that forces us to call this method for the first time
        // in the middle of another action.  That is the theory anyway.
        SVNProviderPlugin.getPlugin().getAdminDirectoryName();
    }

    /**
     * get the current svn client interface used
     * @return
     */
    public String getSvnClientInterface() {
        return svnClientInterface;
    }    
    
	/**
	 * @param configDir The configDir to set.
	 */
	public void setConfigDir(File configDir) {
		this.configDir = configDir;
	}
    
    /**
     * @return a new ISVNClientAdapter depending on the client interface
     * @throws SVNClientException
     */
    public ISVNClientAdapter createSVNClient() throws SVNException {
    	ISVNClientAdapter svnClient = this.getAdapter(svnClientInterface);
    	if (svnClient == null) {
    		svnClient = this.getAdapter(null);
    	}
    	if (svnClient == null)
    		throw new SVNException("No client adapters available.");
    	if (configDir != null) {
    		try {
				svnClient.setConfigDirectory(configDir);
			} catch (SVNClientException e) {
	        	throw SVNException.wrapException(e);
			}
    	} 
    	if (SVNProviderPlugin.getPlugin().getSvnPromptUserPassword() != null)
    	    svnClient.addPasswordCallback(SVNProviderPlugin.getPlugin().getSvnPromptUserPassword());
    	return svnClient;
    }
    
    private ISVNClientAdapter getAdapter(String key) {
    	ISVNClientAdapter client = null;
    	if (key == null) {
    		key = "default";
     	}
    	client = (ISVNClientAdapter) clients.get(key);
    	if (client == null) {
    		if (key.equals("default"))
    	   		client = Activator.getDefault().getAnyClientAdapter();
    		else
    			client = Activator.getDefault().getClientAdapter(svnClientInterface);
    		clients.put(key, client);
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
	
}
