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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * Command to get the statuses of local resources
 */
public class GetStatusCommand implements ISVNCommand {
    private ISVNLocalResource svnResource;
    private boolean descend = true;
    private boolean getAll = true;
    private LocalResourceStatus[] statuses;
    
    public GetStatusCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll) {
        this.svnResource = svnResource;
        this.descend = true;
        this.getAll = getAll;
    }

    public GetStatusCommand(ISVNLocalResource svnResource) {
        this(svnResource, true, true);
    }
    
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        ISVNStatus[] svnStatuses = null;
        ISVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
        try { 
            svnStatuses = svnClient.getStatus(svnResource.getFile(), descend, getAll);
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

    /**
     * get the resource corresponding to the given status
     * @param status
     * @return
     */
    static public IResource getResource(LocalResourceStatus status) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath pathEclipse = new Path(status.getFile().getAbsolutePath());
        
        IResource resource = null;
        if (status.getNodeKind()  == SVNNodeKind.DIR)       
            resource = workspaceRoot.getContainerForLocation(pathEclipse);
        else
            if (status.getNodeKind() == SVNNodeKind.FILE)
                resource =  workspaceRoot.getFileForLocation(pathEclipse);
            else {
                if (status.getNodeKind() == SVNNodeKind.UNKNOWN) {
	            	File file = pathEclipse.toFile();
	            	if (file.isDirectory())
	            		resource = workspaceRoot.getContainerForLocation(pathEclipse);
	            	else
	            		resource = workspaceRoot.getFileForLocation(pathEclipse);
                }
            }
       return resource;     
    }    
    
    /**
     * get the ISVNLocalResource corresponding to the given status
     * @param status
     * @return
     */
    static public ISVNLocalResource getSVNLocalResource(LocalResourceStatus status) {
        IResource resource = getResource(status);
        return SVNWorkspaceRoot.getSVNResourceFor(resource);
    }
     
}
