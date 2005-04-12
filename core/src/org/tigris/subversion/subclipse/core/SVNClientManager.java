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

import org.eclipse.core.internal.resources.IManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.commandline.CmdLineClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;

/**
 * Handles the creation of SVNClients
 * 
 * @author Cedric Chabanois (cchab at tigris.org) 
 */
public class SVNClientManager implements IManager {
    private SVNAdapterFactories adapterFactories;
    private String svnClientInterface;  
    private File configDir = null;
    private boolean fetchChangePathOnDemand = true;
    
    /* (non-Javadoc)
     * @see org.eclipse.core.internal.resources.IManager#startup(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void startup(IProgressMonitor monitor) throws CoreException {
        try {
            JhlClientAdapterFactory.setup();
        } catch (SVNClientException e) {
            // if an exception is thrown, javahl is not available or 
            // already registered ...
            System.out.println("Javahl interface not available");
            SVNProviderPlugin.log(SVNException.wrapException(e));
        }
        try {
            CmdLineClientAdapterFactory.setup();
        } catch (SVNClientException e) {
            // if an exception is thrown, command line interface is not available or 
            // already registered ...
            System.out.println("Command line interface not available");
            SVNProviderPlugin.log(SVNException.wrapException(e));
        }
        
        
        
        // by default, we set the svn client interface to the best available
        // (JNI if available or command line interface)
        try {
            svnClientInterface = SVNClientAdapterFactory.getPreferredSVNClientType();
        } catch (SVNClientException e) {
            throw new CoreException(new Status(Status.ERROR, SVNProviderPlugin.ID, IStatus.OK, e
                    .getMessage(), e));
        }
        
        setFetchChangePathOnDemand(fetchChangePathOnDemand);
    }
    
    
	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.resources.IManager#shutdown(org.eclipse.core.runtime.IProgressMonitor)
	 */
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
        if (SVNClientAdapterFactory.isSVNClientAvailable(svnClientInterface)) {
            this.svnClientInterface = svnClientInterface;
        }
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
        try {
        	ISVNClientAdapter svnClient = SVNClientAdapterFactory.createSVNClient(svnClientInterface);
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
}
