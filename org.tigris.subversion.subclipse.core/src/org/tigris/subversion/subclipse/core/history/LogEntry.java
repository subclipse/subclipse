/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.history;

 
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.PlatformObject;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
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
    private Alias[] tags;
    
    private String messageOverride = null; // Log comment may be overridden  
    private String authorOverride = null; // Author names may be overridden  
    
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    /**
     * creates a LogEntry
     * @param logMessage
     * @param resource the corresponding remote resource or null
     * @param repository
     */
	private LogEntry(
            ISVNLogMessage logMessage,
            ISVNResource resource,
            ISVNRemoteResource remoteResource,
            Alias[] tags) {
        this.logMessage = logMessage;
        this.remoteResource = remoteResource;
        this.resource = resource;
        this.tags = tags;
	}

    /**
     * create the LogEntry for the logMessages
     * @param logMessages
     * @return
     */
    public static ILogEntry[] createLogEntriesFrom(ISVNRemoteFolder remoteFolder, ISVNLogMessage[] logMessages, Tags[] tags) {
        // if we get the history for a folder, we get the history for all
        // its members
    	// so there is no remoteResource associated with each LogEntry
        ILogEntry[] result = new ILogEntry[logMessages.length]; 
        for (int i = 0; i < logMessages.length;i++) {
        	result[i] = new LogEntry(logMessages[i], remoteFolder, null, (tags[i] != null) ? tags[i].getTags() : null); 
        }
        return result;
    }

    /**
     * create the LogEntry for the logMessages
     * @param logMessages
     * @return
     */
    public static ILogEntry[] createLogEntriesFrom(ISVNRemoteFile remoteFile, ISVNLogMessage[] logMessages, Tags[] tags, SVNUrl[] urls) {
        ILogEntry[] result = new ILogEntry[logMessages.length]; 
        for (int i = 0; i < logMessages.length;i++) {
            ISVNLogMessage logMessage = logMessages[i];
            ISVNRemoteResource correspondingResource;
            correspondingResource = new RemoteFile(
                        null,
                        remoteFile.getRepository(), 
                        urls[i], 
                        logMessage.getRevision(), 
                        logMessage.getRevision(), 
                        logMessage.getDate(), 
                        logMessage.getAuthor());  
            result[i] = new LogEntry(logMessage, remoteFile, correspondingResource, (tags[i] != null) ? tags[i].getTags() : null);
        }
        return result;
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
		if (logMessage.getMessage() == null)
		    return "";
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
    		SVNUrl url = resource.getRepository().getRepositoryRoot();
    		if (url == null)
    		    url = updateRootUrl(resource);
    		changePaths = getPathsOnDemand(url);
    		if (changePaths == null) {
    		    // Root URL is probably bad.  Run svn info to retrieve the root URL and
    		    // update it in the repository.
    		    SVNUrl url2 = updateRootUrl(resource);
    		    if (!url.toString().equals(url2.toString()))
    		        changePaths = getPathsOnDemand(url);
    		    // one last try using the resource URL
    		    if (changePaths == null)
    		        changePaths = getPathsOnDemand(resource.getUrl());
    		    
    		    // Still nothing, just return an empty array
    		    if (changePaths == null)
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
    
    /**
     * @param resource
     * @return rootURL
     */
    private SVNUrl updateRootUrl(ISVNResource resource) {
    	ISVNClientAdapter client = null;
        try {
            client = SVNProviderPlugin.getPlugin().getSVNClient();
            SVNProviderPlugin.disableConsoleLogging(); 
            ISVNInfo info = client.getInfo(resource.getUrl());
            SVNProviderPlugin.enableConsoleLogging(); 
            if (info.getRepository() == null)
                return resource.getUrl();
            else {
                // update the saved root URL
                resource.getRepository().setRepositoryRoot(info.getRepository());
                return info.getRepository();
            }
        } catch (Exception e) {
            SVNProviderPlugin.enableConsoleLogging(); 
            return resource.getUrl();
        } finally {
	        SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
        }
    }

    private ISVNLogMessageChangePath[] getPathsOnDemand(SVNUrl url) {
		ISVNLogMessage[] tmpMessage;
		ISVNClientAdapter client = null;
        try {
            client = SVNProviderPlugin.getPlugin().getSVNClient(); // errors will not log to console
            SVNProviderPlugin.disableConsoleLogging(); 
            tmpMessage = client.getLogMessages(url, getRevision(), getRevision(), true);
            SVNProviderPlugin.enableConsoleLogging(); 
	        if (tmpMessage != null && tmpMessage.length > 0)
			    return tmpMessage[0].getChangedPaths();
			else
			    return null;
        } catch (Exception e) {
            SVNProviderPlugin.enableConsoleLogging(); 
            return null;
        } finally {
	        SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
        }
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

	public Alias[] getTags() {
		return tags;
	}

	public void setTags(Alias[] tags) {
		this.tags = tags;
	}
	
	public long getNumberOfChildren() {
		return logMessage.getNumberOfChildren();
	}
	
	public ILogEntry[] getChildMessages() {
		ISVNLogMessage[] childMessages = logMessage.getChildMessages();
		if (childMessages == null) return null;
		ILogEntry[] childEntries = new ILogEntry[childMessages.length];
		for (int i = 0; i < childMessages.length; i++) {
			childEntries[i] = new LogEntry(childMessages[i], resource, remoteResource, null);
		}
		return childEntries;
	}
	
	public String getMergedRevisionsAsString() {
		ILogEntry[] childMessages = getChildMessages();
		if (childMessages == null || childMessages.length == 0) return "";
		StringBuffer mergedRevisions = new StringBuffer();
		for (int i = 0; i < childMessages.length; i++) {
			if (i > 0) mergedRevisions.append(", ");
			mergedRevisions.append(childMessages[i].getRevision().getNumber());
		}
		return mergedRevisions.toString();
	}
	
	public String getChangeLog(boolean includeAffectedPaths) {
		StringBuffer log = new StringBuffer("r" + getRevision());
		if (getAuthor() != null) log.append(" | " + getAuthor());
		if (getDate() != null) log.append(" | " + formatDate());
		if (includeAffectedPaths) {
			log.append("\nChanged paths:");
			LogEntryChangePath[] changePaths = getLogEntryChangePaths();
			for (int i = 0; i < changePaths.length; i++) {
				log.append("\n\t" + changePaths[i].getAction() + " " + changePaths[i].getPath());
			}
		}
		if (getComment() != null && getComment().trim().length() > 0)
			log.append("\n\n" + getComment());
		log.append("\n----------------------------------------------------------------------------\n");
		return log.toString();
	}
	
	public String getGnuLog() {
		StringBuffer log = new StringBuffer();
		if (getDate() != null) log.append(formatDate());
		if (getAuthor() != null) log.append("  " + getAuthor());
		if (getComment() != null && getComment().trim().length() > 0) {
			String tabbedComment = "\t" + getComment().replaceAll("\n", "\n\t");
			log.append("\n\n" + tabbedComment);
		}
		log.append("\n\n");
		return log.toString();
	}
	
	private String formatDate() {
		return format.format(getDate());
	}

}

