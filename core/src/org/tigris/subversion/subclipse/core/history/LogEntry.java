/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion  
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.history;

 
import java.util.Date;

import org.eclipse.core.runtime.PlatformObject;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * represent an entry for a SVN file that results
 * from the svn log command.
 */
public class LogEntry extends PlatformObject implements ILogEntry {

	private ISVNRemoteResource remoteResource; // the corresponding remote resource
    private ISVNLogMessage logMessage;
    private ISVNResource resource; // the resource for which we asked the history
    
    private String messageOverride = null; // Log comment may be overridden  
    private String authorOverride = null; // Author names may be overridden  

    /**
     * creates a LogEntry
     * @param logMessage
     * @param resource the corresponding remote resource or null
     * @param repository
     */
	public LogEntry(
            ISVNLogMessage logMessage,
            ISVNResource resource,
            ISVNRemoteResource remoteResource) {
        this.logMessage = logMessage;
        this.remoteResource = remoteResource;
        this.resource = resource;
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getRevision()
     */
	public SVNRevision.Number getRevision() {
		return logMessage.getRevision();
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getAuthor()
     */
	public String getAuthor() {
		if (authorOverride != null) {
			return authorOverride;
		}
		return logMessage.getAuthor();
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getDate()
     */
	public Date getDate() {
		return logMessage.getDate();
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getComment()
     */
	public String getComment() {
		if (messageOverride != null) {
			return messageOverride;
		}
		return logMessage.getMessage();
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getResource()
     */
    public ISVNResource getResource() {
    	return resource;
    }

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getRemoteResource()
     */
	public ISVNRemoteResource getRemoteResource() {
		return remoteResource;
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.history.ILogEntry#getLogEntryChangePaths()
     */
    public LogEntryChangePath[] getLogEntryChangePaths() {
    	ISVNLogMessageChangePath[] changePaths = null;
    	if (SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand()) {
    	try {
    		ISVNClientAdapter client = resource.getRepository().getSVNClient();
    		SVNUrl url = resource.getRepository().getRepositoryRoot();
    		if (url == null)
    		    url = resource.getRepository().getUrl();
    		try {
	    		ISVNLogMessage[] tmpMessage = client.getLogMessages(url, getRevision(), getRevision(), true);
	    		changePaths = tmpMessage[0].getChangedPaths();
    		} catch(SVNClientException ce) {
    		    // Root URL is probably bad.  Use the repository URL and change the root URL to
    		    // be equal to the repository URL.
    		    url = resource.getRepository().getUrl();
    		    resource.getRepository().setRepositoryRoot(url);
        		ISVNLogMessage[] tmpMessage = client.getLogMessages(url, getRevision(), getRevision(), true);
        		changePaths = tmpMessage[0].getChangedPaths();
    		}
		} catch (SVNException e) {
			e.printStackTrace();
			changePaths = new ISVNLogMessageChangePath[0];
		} catch (SVNClientException e) {
			e.printStackTrace();
			changePaths = new ISVNLogMessageChangePath[0];
		}
    	} else {
    		changePaths = logMessage.getChangedPaths();
    	}
		
        LogEntryChangePath[] logEntryChangePaths = new LogEntryChangePath[changePaths.length]; 
        for (int i = 0; i < changePaths.length; i++) {
        	logEntryChangePaths[i] = new LogEntryChangePath(this,changePaths[i]);
        }
        return logEntryChangePaths;
    }
    

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(remoteResource)) {
			return remoteResource;
		}
		return super.getAdapter(adapter);
	}
	
	/**
	 * Re-sets the comment after changing it as a revision property.
	 * @param newComment
	 */
	public void setComment(String newComment) {
		messageOverride = newComment;
	}

	/**
	 * Re-sets the author after changing it as a revision property.
	 * @param newAuthor
	 */
	public void setAuthor(String newAuthor) {
		authorOverride = newAuthor;
	}

}

