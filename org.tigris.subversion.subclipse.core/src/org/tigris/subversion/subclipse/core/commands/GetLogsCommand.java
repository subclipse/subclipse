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
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.history.Tags;
import org.tigris.subversion.subclipse.core.resources.BaseResource;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNLogMessageCallback;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Command to get the logs of a remote resource
 * 
 */
public class GetLogsCommand implements ISVNCommand {
	
	private ISVNRemoteResource remoteResource;
	private SVNRevision pegRevision = SVNRevision.HEAD;
	private SVNRevision revisionStart = new SVNRevision.Number(0);
	private SVNRevision revisionEnd = SVNRevision.HEAD;
	private boolean stopOnCopy = false;
	private long limit = 0;
	private boolean includeMergedRevisions;
	private AliasManager tagManager;
    private ILogEntry[] logEntries;
    private SVNLogMessageCallback callback;
   
    /**
     * Constructor
     * 
     * @param remoteResource
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @param tagManager    used to determine tags for revision                     
     */
    public GetLogsCommand(ISVNRemoteResource remoteResource, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit, AliasManager tagManager, boolean includeMergedRevisions) {
        this.remoteResource = remoteResource;
        this.pegRevision = (pegRevision != null) ? pegRevision : SVNRevision.HEAD;
        this.revisionStart = revisionStart;
        this.revisionEnd = (revisionEnd != null) ? revisionEnd : SVNRevision.HEAD;
        this.stopOnCopy = stopOnCopy;
        this.limit = limit;
        this.tagManager = tagManager;
        this.includeMergedRevisions = includeMergedRevisions;
    }    
    
    /**
     * execute the command
     * @param aMonitor
     * @throws SVNException
     */
    public void run(IProgressMonitor aMonitor) throws SVNException {
    	ISVNRepositoryLocation repository = null;
    	ISVNClientAdapter svnClient = null;
        logEntries = null;
        IProgressMonitor monitor = Policy.monitorFor(aMonitor);
        monitor.beginTask(Policy.bind("RemoteFile.getLogEntries"), 100); //$NON-NLS-1$
        
        ISVNLogMessage[] logMessages;
        try {
        	if (callback == null) {
	            logMessages = remoteResource.getLogMessages(
	                    pegRevision,
	                    revisionStart,
	                    revisionEnd, 
	                    stopOnCopy,
	                    !SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand(),
	                    limit, includeMergedRevisions);
        	} else {
        		repository = remoteResource.getRepository();
        		svnClient = repository.getSVNClient();
        		if (remoteResource instanceof BaseResource) {
        			boolean logMessagesRetrieved = false;
        			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(remoteResource.getResource());
        			if (svnResource != null) {
        				LocalResourceStatus status = svnResource.getStatus();
        				if (status != null && status.isCopied()) {
        					ISVNInfo info = svnClient.getInfoFromWorkingCopy(svnResource.getFile());
        					SVNUrl copiedFromUrl = info.getCopyUrl();
        					if (copiedFromUrl != null) {
        						svnClient.getLogMessages(copiedFromUrl, SVNRevision.HEAD, revisionStart, revisionEnd, stopOnCopy, !SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand(), limit, includeMergedRevisions, ISVNClientAdapter.DEFAULT_LOG_PROPERTIES, callback);
        						logMessagesRetrieved = true;
        		        		GetRemoteResourceCommand getRemoteResourceCommand = new GetRemoteResourceCommand(remoteResource.getRepository(), copiedFromUrl, SVNRevision.HEAD);
        		        		getRemoteResourceCommand.run(null);
        		        		remoteResource = getRemoteResourceCommand.getRemoteResource();
        					}
        				}
        			}
        			if (!logMessagesRetrieved) svnClient.getLogMessages(((BaseResource)remoteResource).getFile(), pegRevision, revisionStart, revisionEnd, stopOnCopy, !SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand(), limit, includeMergedRevisions, ISVNClientAdapter.DEFAULT_LOG_PROPERTIES, callback);
        		} else {
        			svnClient.getLogMessages(remoteResource.getUrl(), pegRevision, revisionStart, revisionEnd, stopOnCopy, !SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand(), limit, includeMergedRevisions, ISVNClientAdapter.DEFAULT_LOG_PROPERTIES, callback);
        		}
        		logMessages = callback.getLogMessages();
        	}
            if (remoteResource.isFolder()) {
                logEntries = LogEntry.createLogEntriesFrom((ISVNRemoteFolder) remoteResource, logMessages, getTags(logMessages));   
            } else {
            	logEntries = LogEntry.createLogEntriesFrom((ISVNRemoteFile) remoteResource, logMessages, getTags(logMessages), getUrls(logMessages));
            }

        } catch (Exception e) {
            throw SVNException.wrapException(e);
        } finally {
        	if (repository != null) {
        		repository.returnSVNClient(svnClient);
        	}
        	monitor.done();
        }
    }    
    
    public void setCallback(SVNLogMessageCallback callback) {
		this.callback = callback;
	}

	/**
     * get the result of the command
     * @return log entries for the supplied resource and range
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
     * @return an array of corresponding resource urls
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

	private Tags[] getTags(ISVNLogMessage[] logMessages) throws NumberFormatException {
		Tags[] tags = new Tags[logMessages.length]; 
        for (int i = 0; i < logMessages.length;i++) {
        	if (tagManager != null) {
        		String rev = logMessages[i].getRevision().toString();
        		int revNo = Integer.parseInt(rev);
        		tags[i] = new Tags(tagManager.getTags(revNo));
        	}
        }
		return tags;
	}
}
