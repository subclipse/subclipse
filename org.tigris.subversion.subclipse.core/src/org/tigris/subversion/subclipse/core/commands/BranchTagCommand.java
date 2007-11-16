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
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagCommand implements ISVNCommand {
	// selected resource
    private IResource resource; 
    
    private SVNUrl sourceUrl;
    private SVNUrl destinationUrl;
    private boolean createOnServer;
    private String message;
    private SVNRevision revision;
    private boolean makeParents;
    
    private SVNWorkspaceRoot root;

    public BranchTagCommand(SVNWorkspaceRoot root, IResource resource, SVNUrl sourceUrl, SVNUrl destinationUrl, String message, boolean createOnServer, SVNRevision revision) {
        super();
        this.root = root;
        this.resource = resource;
        this.sourceUrl = sourceUrl;
        this.destinationUrl = destinationUrl;
        this.createOnServer = createOnServer;
        this.message = message;
        this.revision = revision;
    }

    public void run(IProgressMonitor monitor) throws SVNException {
        try {
            monitor.beginTask(null, 100);
            ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
//            OperationManager.getInstance().beginOperation(svnClient);
            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor));
            monitor.subTask(resource.getName());
            if (createOnServer) svnClient.copy(sourceUrl, destinationUrl, message, revision, makeParents);
            else {
                File file = resource.getLocation().toFile();
                svnClient.copy(file, destinationUrl, message);
            }
            monitor.worked(100);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
            OperationManager.getInstance().endOperation();
            monitor.done();
        }                
    }

	public void setMakeParents(boolean makeParents) {
		this.makeParents = makeParents;
	}

}
