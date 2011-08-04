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
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeCommand implements ISVNCommand {
	// selected resource
    private IResource resource;  
    
    private SVNUrl svnUrl1;
    private SVNUrl svnUrl2;
    
    private SVNRevision svnRevision1;
    private SVNRevision svnRevision2;
    
    private SVNWorkspaceRoot root;
    
    private boolean force = false;
    private boolean ignoreAncestry = false;
    private boolean recurse = true;
    
    public MergeCommand(SVNWorkspaceRoot root, IResource resource, SVNUrl svnUrl1, SVNRevision svnRevision1, SVNUrl svnUrl2, SVNRevision svnRevision2) {
        super();
        this.root = root;
        this.resource = resource;
        this.svnUrl1 = svnUrl1;
        this.svnRevision1 = svnRevision1;
        this.svnUrl2 = svnUrl2;
        this.svnRevision2 = svnRevision2;               
    }

    public void run(IProgressMonitor monitor) throws SVNException {
    	ISVNClientAdapter svnClient = null;
        try {
            monitor.beginTask(null, 100);
            svnClient = root.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor, svnClient));
            monitor.subTask(resource.getName());
            File file = resource.getLocation().toFile();
            svnClient.merge(svnUrl1, svnRevision1, svnUrl2, svnRevision2, file, force, recurse, false, ignoreAncestry);
            try {
                // Refresh the resource after merge
                resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            } catch (CoreException e1) {
            }
            monitor.worked(100);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	root.getRepository().returnSVNClient(svnClient);
            OperationManager.getInstance().endOperation();
            monitor.done();
        }        
    }
    
	public void setForce(boolean force) {
		this.force = force;
	}

	public void setIgnoreAncestry(boolean ignoreAncestry) {
		this.ignoreAncestry = ignoreAncestry;
	}

	public void setRecurse(boolean recurse) {
		this.recurse = recurse;
	}

}
