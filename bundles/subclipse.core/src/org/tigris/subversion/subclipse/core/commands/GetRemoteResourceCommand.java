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

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Command to get a remote resource given a url and a revision
 */
public class GetRemoteResourceCommand implements ISVNCommand {
	private ISVNRepositoryLocation repository;
    private SVNUrl url;
    private SVNRevision revision;
    
    private ISVNRemoteResource remoteResource; 
    
    /**
     * revision must not be SVNRevision.BASE ! 
     * @param repository
     * @param url
     * @param revision
     */
    public GetRemoteResourceCommand(ISVNRepositoryLocation repository, SVNUrl url, SVNRevision revision) {
       this.repository = repository;
       this.url = url;
       this.revision = revision;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
        monitor = Policy.monitorFor(monitor);
        monitor.beginTask(Policy.bind("GetRemoteResourceCommand.getLogEntries"), 100); //$NON-NLS-1$
        
        remoteResource = null;
        ISVNClientAdapter svnClient = repository.getSVNClient();
        ISVNInfo info;
        try {
            info = svnClient.getInfo(url, revision, revision);
        } catch (SVNClientException e) {
            throw new SVNException("Can't get remote resource "+url+" at revision "+revision,e);   
        }
        finally {
        	repository.returnSVNClient(svnClient);
        }
        
        if (info == null) {
            remoteResource = null; // no remote file
        }
        else
        {
            if (info.getNodeKind() == SVNNodeKind.FILE)
                remoteResource = new RemoteFile(
                    null,  // we don't know its parent
                    repository,
                    url,
                    revision,
                    info.getLastChangedRevision(),
                    info.getLastChangedDate(),
                    info.getLastCommitAuthor()
                );
             else
                remoteResource = new RemoteFolder(
                    null,  // we don't know its parent
                    repository,
                    url,
                    revision,
                    info.getLastChangedRevision(),
                    info.getLastChangedDate(),
                    info.getLastCommitAuthor()
                );                
        }
        monitor.done();
	}

    /**
     * get the result of the command
     * @return
     */    
    public ISVNRemoteResource getRemoteResource() {
        return remoteResource;
    }
    
}
