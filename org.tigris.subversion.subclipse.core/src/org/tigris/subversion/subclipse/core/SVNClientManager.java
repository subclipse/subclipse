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
import java.util.Iterator;
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
    private String svnClientInterface;
    private String svnAdminDir = ".svn";
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
    }

    /**
     * get the current svn client interface used
     * @return
     */
    public String getSvnClientInterface() {
        return svnClientInterface;
    }    
    
    public String getSvnAdminDirectory() {
    	return svnAdminDir;
    }
    
	/**
	 * @param configDir The configDir to set.
	 */
	public void setConfigDir(File configDir) {
		this.configDir = configDir;
//		if (configDir == null) return;
		// Update configDir in stored clients
		Set keys = clients.keySet();
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
	    	ISVNClientAdapter svnClient = (ISVNClientAdapter) clients.get(key);
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
    	svnAdminDir = svnClient.getAdminDirectoryName();
	}
    
    private ISVNClientAdapter getAdapter(String key) throws SVNException {
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
    		setupClientAdapter(client);
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
