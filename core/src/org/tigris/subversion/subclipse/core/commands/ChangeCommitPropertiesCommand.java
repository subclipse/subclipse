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

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Changes the commit comment on a previously committed revision
 * 
 * @author Jesper Steen Møller (jespersm at tigris.org) 
 */
public class ChangeCommitPropertiesCommand implements ISVNCommand {
	private ISVNRepositoryLocation repositoryLocation;
    private SVNRevision.Number revisionNo;
    private String logMessage;
    private String author;
    
    public ChangeCommitPropertiesCommand(ISVNRepositoryLocation theRepositoryLocation, SVNRevision.Number theRevisionNo, String theLogMessage, String theAuthor) {
    	this.repositoryLocation = theRepositoryLocation; 
        this.revisionNo = theRevisionNo;
        this.logMessage = theLogMessage;
        this.author = theAuthor;
    }
        
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
        monitor.beginTask(null, 100); //$NON-NLS-1$

        ISVNClientAdapter svnClient = repositoryLocation.getSVNClient();
        try {
            OperationManager.getInstance().beginOperation(svnClient);
            
            try {
            	if (logMessage != null)
            		svnClient.setRevProperty(repositoryLocation.getUrl(), revisionNo, "svn:log", logMessage, true);
            	if (author != null)
            		svnClient.setRevProperty(repositoryLocation.getUrl(), revisionNo, "svn:author", author, true);
            }
            catch (SVNClientException e) {
                throw SVNException.wrapException(e);
            }

        } finally {
            OperationManager.getInstance().endOperation();
            monitor.done();
        }
	}
}
