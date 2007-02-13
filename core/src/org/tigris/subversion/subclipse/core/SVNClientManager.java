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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.commandline.CmdLineClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.svnkit.SvnKitClientAdapterFactory;

/**
 * Handles the creation of SVNClients
 * 
 * @author Cedric Chabanois (cchab at tigris.org) 
 */
public class SVNClientManager {
    private String svnClientInterface;  
    private File configDir = null;
    private boolean fetchChangePathOnDemand = true;
    private boolean javahl = false;
    private boolean svnkit = false;
    
    public void startup(IProgressMonitor monitor) throws CoreException {
    }
    
    
	public void shutdown(IProgressMonitor monitor) throws CoreException {
	}

    /**
     * set the client interface to use, either
     * SVNClientAdapterFactory.JAVAHL_CLIENT or
     * SVNClientAdapterFactory.SVNCOMMANDLINE_CLIENT
     * 
     * @param svnClientInterface
     */
    public void setSvnClientInterface(String svnClientInterface) {
        if (svnClientInterface == null) {
          // if no specific interface is specified, load JavaHL
          // if JavaHL fails to load, then JavaSVN will load
            loadJavaHLAdapter();
            try {
                this.svnClientInterface = SVNClientAdapterFactory.getPreferredSVNClientType();
            } catch (SVNClientException e) {
                SVNProviderPlugin.log(new TeamException(new Status(IStatus.ERROR, SVNProviderPlugin.ID, IStatus.OK, e
                        .getMessage(), e)));
                return;
            }
        } else {
	        if (CmdLineClientAdapterFactory.COMMANDLINE_CLIENT.equals(svnClientInterface))
                svnClientInterface = SvnKitClientAdapterFactory.SVNKIT_CLIENT;
	        if (JhlClientAdapterFactory.JAVAHL_CLIENT.equals(svnClientInterface))
	            loadJavaHLAdapter();
	        if (SvnKitClientAdapterFactory.SVNKIT_CLIENT.equals(svnClientInterface))
	            loadSVNKitAdapter();
	        if ("javasvn".equals(svnClientInterface))
	            loadSVNKitAdapter();
	        if (SVNClientAdapterFactory.isSVNClientAvailable(svnClientInterface)) {
	            this.svnClientInterface = svnClientInterface;
	        } else {
	            if (this.svnClientInterface == null && SVNClientAdapterFactory.isSVNClientAvailable(SvnKitClientAdapterFactory.SVNKIT_CLIENT))
	                this.svnClientInterface = SvnKitClientAdapterFactory.SVNKIT_CLIENT;
	        }
        }
        
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
        if (svnClientInterface == null)
            // force the adapters to load
            setSvnClientInterface(null);
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
        try {
        	ISVNClientAdapter svnClient = SVNClientAdapterFactory.createSVNClient(getSvnClientInterface());
        	if (configDir != null) {
        		svnClient.setConfigDirectory(configDir);
        	} 
        	if (SVNProviderPlugin.getPlugin().getSvnPromptUserPassword() != null)
        	    svnClient.addPasswordCallback(SVNProviderPlugin.getPlugin().getSvnPromptUserPassword());
        	return svnClient;
        } catch (SVNClientException e) {
        	throw SVNException.wrapException(e);
        }
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
	
	public void loadAdapters() {
	    loadJavaHLAdapter();
	    loadSVNKitAdapter();
	}
	
	private void loadJavaHLAdapter() {
        if (!javahl) {
            javahl = true;
		    try {
	            JhlClientAdapterFactory.setup();
	        } catch (SVNClientException e) {
	            loadSVNKitAdapter(); // try to make sure there is at least one adapter available
	        }
        }
	}
	
	private void loadSVNKitAdapter() {
        if (!svnkit) {
            svnkit = true;
		    try {
	            SvnKitClientAdapterFactory.setup();
	        } catch (SVNClientException e) {
	        }
        }
	}
}
