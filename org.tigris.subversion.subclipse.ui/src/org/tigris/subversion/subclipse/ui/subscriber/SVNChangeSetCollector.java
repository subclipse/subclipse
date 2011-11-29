/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SVNChangeSetCollector extends SyncInfoSetChangeSetCollector {

	/**
	 * Change set used to store incoming changes in
	 */
	public class SVNCheckedInChangeSet extends CheckedInChangeSet {
		
		private long revision;
		private String author;
		private Date date;
		private String comment;

		/**
		 * Create a checked in change set from the given syncinfo
		 * @param info syncinfo to create change set from
		 */
		public SVNCheckedInChangeSet(SyncInfo info) {
			this(new SyncInfo[] { info });
		}
		
		/**
		 * Create a checked in change set from the given syncinfos
		 * @param infos syncinfos to create change set from
		 */
		public SVNCheckedInChangeSet(SyncInfo[] infos) {
			super();
			add(infos);
			initData();
			String formattedDate;
			if (date == null)
				formattedDate = "n/a";
			else
			    formattedDate = DateFormat.getInstance().format(date);
			setName(revision + "  [" + author + "]  (" + formattedDate + ")  " + comment);
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.team.internal.core.subscribers.CheckedInChangeSet#getAuthor()
		 */
	    public String getAuthor() {
	    	return author;
	    }
	    
	    /**
	     * Set the author of the change set
	     */
	    public void setAuthor(String author) {
	    	this.author = author;
	    }
	    
	    /*
	     * (non-Javadoc)
	     * @see org.eclipse.team.internal.core.subscribers.CheckedInChangeSet#getDate()
	     */
	    public Date getDate() {
	    	return date;
	    }

	    /**
	     * Sets the date of the change set
	     */
	    public void setDate(Date date) {
	    	this.date = date;
	    }
	    
	    /*
	     * (non-Javadoc)
	     * @see org.eclipse.team.internal.core.subscribers.ChangeSet#getComment()
	     */
	    public String getComment() {
	    	return comment;
	    }
	    
	    /**
	     * Sets the comment of the change set
	     * @param comment
	     */
	    public void setComment(String comment) {
	    	this.comment = comment;
	    }
	    
	    /**
	     * Returns the revision of this checked in change set
	     * @return revision of the change set
	     */
	    public long getRevision() {
	    	return revision;
	    }
	    
	    /**
	     * Initialize the data of this checked in change set
	     */
	    private void initData() {
	    	revision = SVNRevision.SVN_INVALID_REVNUM;
	    	SyncInfoTree syncInfoTree = getSyncInfoSet();
	    	SyncInfo[] syncInfos = syncInfoTree.getSyncInfos();
	    	if (syncInfos.length > 0) {
	    		SyncInfo syncInfo = syncInfos[0];
	    		if (syncInfo instanceof SVNStatusSyncInfo) {
	    			SVNStatusSyncInfo svnSyncInfo = (SVNStatusSyncInfo)syncInfo;
	    			RemoteResourceStatus remoteResourceStatus = svnSyncInfo.getRemoteResourceStatus();
	    			if (remoteResourceStatus != null) {
		    			SVNRevision.Number revnum = remoteResourceStatus.getLastChangedRevision();
		    			if (revnum != null)
		    				revision = revnum.getNumber();
		    			else
		    				revision = SVNRevision.INVALID_REVISION.getNumber();
		    			author = remoteResourceStatus.getLastCommitAuthor();
		    			if ((author == null) || (author.length() == 0)) {
		    				author = Policy.bind("SynchronizeView.noAuthor"); //$NON-NLS-1$
		    			}
		    			date = remoteResourceStatus.getLastChangedDate();
		    			comment = fetchComment(svnSyncInfo);
	    			} else {
	    				revision = SVNRevision.INVALID_REVISION.getNumber();
	    				author = Policy.bind("SynchronizeView.noAuthor"); //$NON-NLS-1$
	    				comment = "";
	    				date = null;
	    			}
	    		}
	    	}
	    }
	    
	    /**
	     * Fetch the comment of the given SyncInfo
	     * @param info info to get comment for
	     * @return the comment
	     */
	    private String fetchComment(SVNStatusSyncInfo info) {
			String fetchedComment = Policy.bind("SynchronizeView.standardIncomingChangeSetComment"); // $NON-NLS-1$
	    	IResourceVariant remoteResource = info.getRemote();
	    	if (remoteResource instanceof ISVNRemoteResource) {
	    		ISVNRemoteResource svnRemoteResource = (ISVNRemoteResource)remoteResource;
	    		ISVNClientAdapter client = null;
	    		try {
					client = svnRemoteResource.getRepository().getSVNClient();
		    		SVNUrl url = svnRemoteResource.getRepository().getRepositoryRoot();
		    		SVNRevision rev = svnRemoteResource.getLastChangedRevision();
		    		ISVNLogMessage[] logMessages = client.getLogMessages(url, rev, rev, false);
					if (logMessages.length != 0) {
						String logComment = logMessages[0].getMessage();
						if (logComment.trim().length() != 0) {
							fetchedComment = flattenComment(logComment); 
						} else {
							fetchedComment = "";
						}
					}
				} catch (SVNException e1) {
					if (!e1.operationInterrupted()) {
						SVNUIPlugin.log(e1);
					}
				} catch (SVNClientException e) {
					SVNUIPlugin.log(SVNException.wrapException(e));
				}
	    		finally {
	    			svnRemoteResource.getRepository().returnSVNClient(client);
	    		}
    		}
	    	return fetchedComment;
	    }
	    
	}
	
	/**
	 * Constructs a new SVNChangeSetCollector used to collect incoming
	 *  change sets
	 */
	public SVNChangeSetCollector(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector#add(org.eclipse.team.core.synchronize.SyncInfo[])
	 */
	protected void add(final SyncInfo[] infos) {
		final Map sets = new HashMap();

		Job job = new Job(Policy.bind("SynchronizeView.collectingChangeSets")) {
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(null, infos.length);
        for (int i=0; i<infos.length; i++) {
          SyncInfo syncInfo = infos[i];
          
          if (syncInfo instanceof SVNStatusSyncInfo) {
            SVNStatusSyncInfo svnSyncInfo = (SVNStatusSyncInfo)syncInfo;
            if (SyncInfo.getDirection(svnSyncInfo.getKind()) == SyncInfo.INCOMING && svnSyncInfo.getRemote() != null) {
              SVNCheckedInChangeSet changeSet = (SVNCheckedInChangeSet) sets.get(svnSyncInfo.getRemote().getContentIdentifier());
              if (changeSet == null) {
                changeSet = new SVNCheckedInChangeSet(svnSyncInfo);
                sets.put(svnSyncInfo.getRemote().getContentIdentifier(), changeSet);
              } else {
                changeSet.add(svnSyncInfo);
              }
            }
          }
          monitor.worked(1);
        }
        monitor.done();

        performUpdate(new IWorkspaceRunnable() {
          public void run(IProgressMonitor monitor) throws CoreException {
            for (Iterator it = sets.values().iterator(); it.hasNext();) {
              SVNCheckedInChangeSet set = (SVNCheckedInChangeSet) it.next();
              SVNChangeSetCollector.this.add(set);
            }
          }
        }, true, new NullProgressMonitor());
        
        return Status.OK_STATUS;
      }
		};
		
		job.schedule();
		try {
      job.join();
    } catch (InterruptedException ex) {
      //
    }
	}
	
	/**
	 * Flatten the given string so it contains no more line breaks
	 * @param string String to strip linebreaks from
	 * @return the string without any line breaks in it
	 */
	private String flattenComment(String string) {
		StringBuffer buffer = new StringBuffer(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(Policy.bind("separator")); //$NON-NLS-1$
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.ChangeSetManager#initializeSets()
	 */
	protected void initializeSets() {
		// Nothing to initialize
	}
	
}
