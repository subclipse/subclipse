/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Command to get the logs of a remote resource
 */
public class GetLogsCommand implements ISVNCommand {
	private ISVNRemoteResource remoteResource;
	private SVNRevision pegRevision = SVNRevision.HEAD;
	private SVNRevision revisionStart = new SVNRevision.Number(0);
	private SVNRevision revisionEnd = SVNRevision.HEAD;
	private boolean stopOnCopy = false;
	private long limit = 0;
	private AliasManager tagManager;
    private ILogEntry[] logEntries;
    
    public GetLogsCommand(ISVNRemoteResource remoteResource) {
        this.remoteResource = remoteResource;
    }
    
    
    /**
     * execute the command
     * @param monitor
     * @throws SVNException
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        logEntries = null;
        ISVNClientAdapter client = remoteResource.getRepository().getSVNClient();
        monitor = Policy.monitorFor(monitor);
        monitor.beginTask(Policy.bind("RemoteFile.getLogEntries"), 100); //$NON-NLS-1$
        
        ISVNLogMessage[] logMessages;
        try {
        	// Conditional behavior to retieve the log messages 
        	if (SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand()) {
            logMessages =
                client.getLogMessages(
                    remoteResource.getUrl(),
                    pegRevision,
                    revisionStart,
                    revisionEnd, 
                    stopOnCopy,
                    false,
                    limit);
        	} else {
        		logMessages =
                    client.getLogMessages(
                        remoteResource.getUrl(),
                        pegRevision,
                        revisionStart,
                        revisionEnd, 
                        stopOnCopy,
                        true,
                        limit);	
        	}
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        }
        
        if (remoteResource.isFolder()) {
            // if we get the history for a folder, we get the history for all
            // its members
            logEntries = createLogEntriesForFolder(logMessages);   
        } else {
        	logEntries = createLogEntriesForFile(logMessages);
        }
             
        monitor.done();
    }    
    
    /**
     * get the result of the command
     * @return
     */
    public ILogEntry[] getLogEntries() {
    	return logEntries;
    }
    
    private SVNUrl[] fillUrlsWith(SVNUrl[] urls, SVNUrl url) {
    	for (int i = 0; i < urls.length;i++) {
    		urls[i] = url;
        }
        return urls;
    }
    
    /**
     * get the urls of the resource for each revision in logMessages
     * It will always be the same url if the resource has never been moved
     * @param logMessages
     * @return
     */
    private SVNUrl[] getUrls(ISVNLogMessage[] logMessages) {
        SVNUrl[] urls = new SVNUrl[logMessages.length];
        
        SVNUrl rootRepositoryUrl = remoteResource.getRepository().getRepositoryRoot();
        if (rootRepositoryUrl == null) {
        	// don't know the root repository url, we consider that resource has never been moved
            // and so that the url was always the same
            return fillUrlsWith(urls, remoteResource.getUrl());
        }
        
        // we identify the logMessage corresponding to the revision
        // of the remote resource
        int indexRemote = -1;
        for (int i = 0; i < logMessages.length;i++) {
            if (logMessages[i].getRevision().equals(remoteResource.getLastChangedRevision())) {
                indexRemote = i;
            	break;
            }
        }
        if(indexRemote > -1) {
            urls[indexRemote] = remoteResource.getUrl();
        }
        
            // we get the url of more recent revisions
            SVNUrl currentUrl = remoteResource.getUrl();
            for (int i = indexRemote+1; i < logMessages.length;i++) {
                ISVNLogMessageChangePath[] changePaths = logMessages[i].getChangedPaths();
                for (int j = 0; j < changePaths.length;j++) {
                	SVNUrl urlChangedPath = rootRepositoryUrl.appendPath(changePaths[j].getPath());
                    if (currentUrl.equals(urlChangedPath)) {
                    	urls[i] = currentUrl;
                        break;
                    }
                    if (changePaths[j].getCopySrcPath() != null) {
                    	SVNUrl urlCopyPath = rootRepositoryUrl.appendPath(changePaths[j].getCopySrcPath());
                        if (currentUrl.equals(urlCopyPath)) {
                        	currentUrl = rootRepositoryUrl.appendPath(changePaths[j].getPath());
                            urls[i] = currentUrl;
                            break;
                        }
                    }
                }
                if (urls[i] == null) {
                	// something went wrong
                    return fillUrlsWith(urls, remoteResource.getUrl());
                }
            }
            
            // we get the url of previous revisions
            currentUrl = remoteResource.getUrl();
            for (int i = indexRemote-1; i >= 0;i--) {
                ISVNLogMessageChangePath[] changePaths = logMessages[i].getChangedPaths();
                for (int j = 0; j < changePaths.length;j++) {
                    SVNUrl urlChangedPath = rootRepositoryUrl.appendPath(changePaths[j].getPath());
                    if (currentUrl.equals(urlChangedPath)) {
                        urls[i] = currentUrl;
    
                        if (changePaths[j].getCopySrcPath() != null) {
                            SVNUrl urlCopyPath = rootRepositoryUrl.appendPath(changePaths[j].getCopySrcPath());
                            currentUrl = urlCopyPath;
                        }
                        break;
                    }
                }
                if (urls[i] == null) {
                    // something went wrong
                    return fillUrlsWith(urls, remoteResource.getUrl());
                }
            }
        return urls;
    }
    
    /**
     * create the LogEntry for the logMessages
     * @param logMessages
     * @return
     */
    private ILogEntry[] createLogEntriesForFolder(ISVNLogMessage[] logMessages) {
        // if we get the history for a folder, we get the history for all
        // its members
    	// so there is no remoteResource associated with each LogEntry
        ILogEntry[] result = new ILogEntry[logMessages.length]; 
        for (int i = 0; i < logMessages.length;i++) {
        	result[i] = new LogEntry(logMessages[i], remoteResource, null); 
        	if (tagManager != null) {
        		String rev = result[i].getRevision().toString();
        		int revNo = Integer.parseInt(rev);
        		result[i].setAliases(tagManager.getAliases(revNo));
        	}
        }
        return result;
    }
    
    /**
     * create the LogEntry for the logMessages
     * @param logMessages
     * @return
     */
    private ILogEntry[] createLogEntriesForFile(ISVNLogMessage[] logMessages) {
        SVNUrl[] urls = getUrls(logMessages);
        ILogEntry[] result = new ILogEntry[logMessages.length]; 
        for (int i = 0; i < logMessages.length;i++) {
            ISVNLogMessage logMessage = logMessages[i];
            ISVNRemoteResource correspondingResource;
            correspondingResource = new RemoteFile(
                        null,
                        remoteResource.getRepository(), 
                        urls[i], 
                        logMessage.getRevision(), 
                        logMessage.getRevision(), 
                        logMessage.getDate(), 
                        logMessage.getAuthor());  
            result[i] = new LogEntry(logMessage, remoteResource, correspondingResource);
        	if (tagManager != null) {
        		String rev = result[i].getRevision().toString();
        		int revNo = Integer.parseInt(rev);
        		result[i].setAliases(tagManager.getAliases(revNo));
        	}        
        }
        return result;
    }


	public void setLimit(long limit) {
		this.limit = limit;
	}


	public void setRevisionEnd(SVNRevision revisionEnd) {
		this.revisionEnd = revisionEnd;
	}


	public void setRevisionStart(SVNRevision revisionStart) {
		this.revisionStart = revisionStart;
	}


	public void setStopOnCopy(boolean stopOnCopy) {
		this.stopOnCopy = stopOnCopy;
	}


	public void setPegRevision(SVNRevision pegRevision) {
		this.pegRevision = pegRevision;
	}


	public void setTagManager(AliasManager tagManager) {
		this.tagManager = tagManager;
	}
    
    
}
