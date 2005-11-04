/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
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
import org.tigris.subversion.svnclientadapter.javasvn.JavaSvnClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;

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
    private boolean javasvn = false;
    private boolean cli = false;
    
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
	        if (JhlClientAdapterFactory.JAVAHL_CLIENT.equals(svnClientInterface))
	            loadJavaHLAdapter();
	        if (JavaSvnClientAdapterFactory.JAVASVN_CLIENT.equals(svnClientInterface))
	            loadJavaSVNAdapter();
	        if (CmdLineClientAdapterFactory.COMMANDLINE_CLIENT.equals(svnClientInterface))
	            loadCmdLineAdapter();
	        if (SVNClientAdapterFactory.isSVNClientAvailable(svnClientInterface)) {
	            this.svnClientInterface = svnClientInterface;
	        } else {
	            if (this.svnClientInterface == null && SVNClientAdapterFactory.isSVNClientAvailable(JavaSvnClientAdapterFactory.JAVASVN_CLIENT))
	                this.svnClientInterface = JavaSvnClientAdapterFactory.JAVASVN_CLIENT;
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
	    loadJavaSVNAdapter();
	    loadCmdLineAdapter();
	}
	
	private void loadJavaHLAdapter() {
        if (!javahl) {
            javahl = true;
		    try {
	            JhlClientAdapterFactory.setup();
	        } catch (SVNClientException e) {
	            loadJavaSVNAdapter(); // try to make sure there is at least one adapter available
	        }
        }
	}
	
	private void loadJavaSVNAdapter() {
        if (!javasvn) {
            javasvn = true;
		    try {
	            JavaSvnClientAdapterFactory.setup();
	        } catch (SVNClientException e) {
	        }
        }
	}
	
	private void loadCmdLineAdapter() {
        if (!cli) {
            cli = true;
		    try {
	            CmdLineClientAdapterFactory.setup();
	        } catch (SVNClientException e) {
	            loadJavaSVNAdapter(); // try to make sure there is at least one adapter available
	        }
        }
	}
}
