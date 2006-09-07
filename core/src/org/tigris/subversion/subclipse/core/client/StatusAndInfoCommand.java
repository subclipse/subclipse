/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.SVNUrlUtils;

/**
 * svn status + subsequent svn info command(s).
 * After execute() of superclass StatusCommand, not all necessary information is retrieved.
 * For added files or resources (incoming additions), their e.g. nodeKind is not known from the status.
 * To get that information, subsequent calls to "svn info" are executed for those unknown urls ...
 * 
 * @author Martin Letenay
 */
public class StatusAndInfoCommand extends StatusCommand implements ISVNCommand {
	
	private RemoteResourceStatus[] remoteStatuses;
	private ISVNLocalResource svnResource;
	private SVNUrl rootUrl;
	private String rootPath;

    public StatusAndInfoCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll, boolean contactServer) {
        super(svnResource.getFile(), descend, getAll, contactServer);
        this.svnResource = svnResource;
        this.rootUrl = svnResource.getWorkspaceRoot().getLocalRoot().getUrl();
        this.rootPath = svnResource.getWorkspaceRoot().getLocalRoot().getFile().getAbsolutePath();
    }

    protected void execute(final ISVNClientAdapter client, final IProgressMonitor monitor) throws SVNClientException {
        super.execute(client, monitor);
        monitor.worked(50);        
        remoteStatuses = collectRemoteStatuses(getStatuses(), client, Policy.subMonitorFor(monitor,50));
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        try { 
            ISVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
            execute(svnClient, monitor);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        }
    }

    /**
     * Answer sorted array of informed statuses ... 
     * @return
     */
    public RemoteResourceStatus[] getRemoteResourceStatuses()
    {
    	return remoteStatuses;
    }

    private RemoteResourceStatus[] collectRemoteStatuses(ISVNStatus[] statuses, ISVNClientAdapter client, final IProgressMonitor monitor)
    {
    	monitor.beginTask("", statuses.length);
    	try {
    	RemoteResourceStatus[] result = new RemoteResourceStatus[statuses.length];

        Arrays.sort(statuses, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((ISVNStatus) o1).getPath().compareTo(((ISVNStatus) o2).getPath());
			}            	
        });

        for (int i = 0; i < statuses.length; i++) {
        	if ((SVNStatusKind.UNVERSIONED.equals(statuses[i].getTextStatus()) || (SVNStatusKind.ADDED.equals(statuses[i].getTextStatus()))))
        	{
        		result[i] = RemoteResourceStatus.NONE;
        	}
        	else
        	{
        		RemoteResourceStatus remoteStatus = new RemoteResourceStatus(statuses[i], getRevisionFor(statuses[i]));
                result[i] = ensureStatusContainsRemoteData(remoteStatus, client, monitor);
        	}
        	monitor.worked(1);
        }	
        
        return result;
    	} finally {
    		monitor.done();
    	}
    }

    /**
     * Ensure that the supplied <code>remoteStatus</code> contains all required data,
     * (I.e. nodeKind for incoming resources, repository's lastChangedRevision for incoming modifications etc ... 
     * @param remoteStatus
     * @return
     */
    private RemoteResourceStatus ensureStatusContainsRemoteData(RemoteResourceStatus remoteStatus, final ISVNClientAdapter client, final IProgressMonitor monitor)
    {
    	
    	//Some clientAdpater implementations (e.g. JavaSVN) do their job right, so there's no need to fetch additional data.
    	if (client.statusReturnsRemoteInfo() & remoteStatus.getLastChangedRevision() != null)
    	{
    		return remoteStatus;
    	}
    	
    		
		if (	//If some crucial data missing ..
				(remoteStatus.getNodeKind() == SVNNodeKind.UNKNOWN) ||
				(remoteStatus.getLastChangedDate() == null) ||
				(remoteStatus.getLastChangedRevision() == null) ||
				(remoteStatus.getLastCommitAuthor() == null) ||
				(remoteStatus.getRepositoryRevision() == null) ||
				(remoteStatus.getUrlString() == null) ||
				//For outgoing changes we don't need to obtain server revisions ...
				((SVNStatusKind.NONE != remoteStatus.getStatusKind()) &&
				 (SVNStatusKind.NORMAL != remoteStatus.getStatusKind()) &&
				 (SVNStatusKind.IGNORED != remoteStatus.getStatusKind()))			
			)
		{
			client.getNotificationHandler().disableLog();
			remoteStatus.updateFromInfo(fetchInfo(client, remoteStatus, monitor));
			client.getNotificationHandler().enableLog();
		}
    	
    	return remoteStatus;
    }
    
    /**
     * Fetch SVNInfo for the supplied remote resource
     * 
     * @param client svnAdapter to run "info" command on
     * @param statuse - RemoteResourceStatus which nodeKinds we want to get and set
     * @throws SVNClientException
     */
    private ISVNInfo fetchInfo(ISVNClientAdapter client, RemoteResourceStatus status, final IProgressMonitor monitor)
    {
    	if (SVNStatusKind.DELETED.equals(status.getTextStatus())) return null;
    	SVNUrl url = status.getUrl();
    	if (url == null)
    	{
    		url = SVNUrlUtils.getUrlFromLocalFileName(status.getPathString(), rootUrl, rootPath);
    	}
        try {
        	monitor.subTask(url.toString());
        	if (SVNStatusKind.EXTERNAL.equals(status.getStatusKind()))
        	{
        		return client.getInfoFromWorkingCopy(status.getFile());
        	}
        	else
        	{
        		return client.getInfo(url);
        	}
        } catch (SVNClientException e) {
            return null;
        }        
    }
}
