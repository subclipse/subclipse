/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNUrlUtils;

/**
 * svn status + subsequent svn info command(s).
 * After execute() of superclass StatusCommand, not all necessary information is retrieved.
 * For added files or resources (incoming additions), their e.g. nodeKind is not known from the status.
 * To get that information, subsequent calls to "svn info" are executed for those unknown urls ...
 * 
 * @author Martin Letenay
 */
public class StatusAndInfoCommand extends StatusCommand implements ISVNCommand {
	
	private RemoteResourceStatus[] informedStatuses;
	private ISVNLocalResource svnResource;
	private SVNUrl rootUrl;
	private String rootPath;

    public StatusAndInfoCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll, boolean contactServer) {
        super(svnResource.getFile(), descend, getAll, contactServer);
        this.svnResource = svnResource;
        this.rootUrl = svnResource.getWorkspaceRoot().getLocalRoot().getUrl();
        this.rootPath = svnResource.getWorkspaceRoot().getLocalRoot().getFile().getAbsolutePath();
    }

    public void execute(final ISVNClientAdapter client, final IProgressMonitor monitor) throws SVNClientException {
        super.execute(client, monitor);
        monitor.worked(50);
        
        informedStatuses = collectRemoteStatuses(getStatuses(), client, Policy.subMonitorFor(monitor,50));
//        informedStatuses = collectInformedStatuses(getStatuses());
//        fetchNodeKinds(client, collectUnknownKinds(informedStatuses));
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
    	return informedStatuses;
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
        	if ((SVNStatusKind.UNVERSIONED == statuses[i].getTextStatus() && (!SVNStatusKind.ADDED.equals(statuses[i].getTextStatus()))))
        	{
        		result[i] = RemoteResourceStatus.NONE;
        	}
        	else
        	{
        		RemoteResourceStatus remoteStatus = new RemoteResourceStatus(statuses[i]);
//        		if ((remoteStatus.getNodeKind() == SVNNodeKind.UNKNOWN) ||
//        				(remoteStatus.getLastChangedDate() == null) ||
//						(remoteStatus.getLastChangedRevision() == null) ||
//						(remoteStatus.getLastCommitAuthor() == null) ||
//						(remoteStatus.getRevision() == null) ||
//						(remoteStatus.getUrlString() == null))
//        		{
        			remoteStatus.updateFromInfo(fetchInfo(client, remoteStatus, monitor));
//        		}
                result[i] = remoteStatus;
        	}
        	monitor.worked(1);
        }	
        
        return result;
    	} finally {
    		monitor.done();
    	}
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
    	ISVNInfo info;
        try {
        	monitor.subTask(url.toString());
            return client.getInfo(url);
        } catch (SVNClientException e) {
            return null;
        }
    }    

    /**
     * Fetch nodeKind info for all InformedStatuses
     * 
     * @param client svnAdapter to run "list" command on
     * @param statuses - list of InformedStatuses which nodeKinds we want to get and set
     * @throws SVNClientException
     * @deprecated not used anymore - was used as part of collecting missing nodeKinds 
     */
    private void fetchNodeKinds(ISVNClientAdapter client, RemoteResourceStatus[] statuses) throws SVNClientException
    {
    	for (int i = 0; i < statuses.length; i++) {
    		if (!SVNStatusKind.UNVERSIONED.equals(statuses[i].getTextStatus()))
    		{
    			fetchNodeKind(client, statuses[i]);
    		}
		}    	
    }

    /**
     * Fetch nodeKind info for the InformedStatus
     * 
     * @param client svnAdapter to run "info" command on
     * @param statuse - RemoteResourceStatus which nodeKinds we want to get and set
     * @throws SVNClientException
     * @deprecated not used anymore - was used as part of collecting missing nodeKinds 
     */
    private void fetchNodeKind(ISVNClientAdapter client, RemoteResourceStatus status)
    {
    	SVNUrl url = SVNUrlUtils.getUrlFromLocalFileName(status.getPathString(), rootUrl, rootPath);
    	ISVNInfo info;
        try {
            info = client.getInfo(url);
        } catch (SVNClientException e) {
            info = null;
        }
        if (info != null)
    	{
    		status.setNodeKind(info.getNodeKind());
    	}
    }    

    /**
     * 
     * @param statuses
     * @return
     * @deprecated not used anymore - was used as part of collecting missing nodeKinds 
     */
    private RemoteResourceStatus[] collectInformedStatuses(ISVNStatus[] statuses)
    {
        Set containerSet = new HashSet();
        List allStatuses = new ArrayList();

        Arrays.sort(statuses, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((ISVNStatus) o1).getPath().compareTo(((ISVNStatus) o2).getPath());
			}            	
        });

        //Collect changed resources (in reverse order so dirs are properly identified
        for (int i = statuses.length - 1; i >= 0; i--) {
            ISVNStatus status = statuses[i];            
            RemoteResourceStatus informedStatus = new RemoteResourceStatus(status, getRevision());
            int resourceType = SVNWorkspaceRoot.getResourceType(status.getPath());
            if ( SVNNodeKind.UNKNOWN  == status.getNodeKind() ) 
            {
                if( resourceType != resourceType)
                {
                	if (IResource.FILE == resourceType)
                	{
                		informedStatus.setNodeKind(SVNNodeKind.FILE);
                	}
                	else if(IResource.FOLDER == resourceType)
                	{
                		informedStatus.setNodeKind(SVNNodeKind.DIR);                		
                	}
                }
                else
                {
                	// check whether the resource path was not already added as parent of a previous resource.
                	// if it was then it is pretty clear that it is directory ...
                	if( containerSet.contains( status.getPath() ) )
                	{
                		informedStatus.setNodeKind(SVNNodeKind.DIR);                		
                	}
                }
            }
            int lastIndexOfSlash = status.getPath().lastIndexOf('/');
            if (lastIndexOfSlash > 0)
            {
            	containerSet.add(status.getPath().substring(0,lastIndexOfSlash));
            }
            allStatuses.add(informedStatus);
        }
        
        RemoteResourceStatus[] result = new RemoteResourceStatus[allStatuses.size()];
        int i= 0;
        //In reverse-reverse order so dir syncInfos are created sooner then files ...
        for (ListIterator iter = allStatuses.listIterator(allStatuses.size()); iter.hasPrevious(); i++) {
        	result[i] = (RemoteResourceStatus) iter.previous();
		}
        
        return result;
    }
    
    /**
     * 
     * @param informedStatuses
     * @return
     * @deprecated not used anymore - was used as part of collecting missing nodeKinds 
     */
    private RemoteResourceStatus[] collectUnknownKinds(RemoteResourceStatus[] informedStatuses)
    {
    	List unknowns = new ArrayList();
    	for (int i = 0; i < informedStatuses.length; i++) {
			if (SVNNodeKind.UNKNOWN == informedStatuses[i].getNodeKind())
			{
				unknowns.add(informedStatuses[i]);
			}
		}
    	return (RemoteResourceStatus[]) unknowns.toArray(new RemoteResourceStatus[0]);
    }
}
