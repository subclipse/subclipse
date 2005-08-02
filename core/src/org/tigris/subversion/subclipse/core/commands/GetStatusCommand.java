/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Command to get the statuses of local resources
 */
public class GetStatusCommand implements ISVNCommand {
    private ISVNRepositoryLocation repository;
    private File file;
    private boolean descend = true;
    private boolean getAll = true;
    private LocalResourceStatus[] statuses;
    
    public GetStatusCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll) {
    	this(svnResource.getRepository(), svnResource.getFile(), descend, getAll);
    }

    public GetStatusCommand(ISVNRepositoryLocation repository, File file, boolean descend, boolean getAll) {
    	this.repository = repository;
    	this.file = file;
        this.descend = descend;
        this.getAll = getAll;
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        ISVNStatus[] svnStatuses = null;
        ISVNClientAdapter svnClient = repository.getSVNClient();
        try { 
            svnStatuses = svnClient.getStatus(file, descend, getAll);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        }
        statuses = convert(svnStatuses);
        
        // we calculated the statuses of some resources. We update the cache manager
        // so that it does not have to redo the status retrieving itself
        SVNProviderPlugin.getPlugin().getStatusCacheManager().setStatuses(statuses);
    }

    private LocalResourceStatus[] convert(ISVNStatus[] svnStatuses) {
        LocalResourceStatus[] localStatuses = new LocalResourceStatus[svnStatuses.length];
        for (int i = 0; i < svnStatuses.length;i++) {
            localStatuses[i] = new LocalResourceStatus(svnStatuses[i]);
        }
        return localStatuses;
    }
    
    /**
     * get the results
     * @return
     */
    public LocalResourceStatus[] getStatuses() {
        return statuses;
    }    
}
