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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNUrlUtils;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

/**
 * svn status + subsequent svn info command(s).
 * After execute() of superclass StatusCommand, not all necessary information is retrieved.
 * For added files or resources (incoming additions), their nodeKind is not known from status.
 * To get that information, subsequent calls to "svn info" are executed for those unknown urls ...
 * 
 * @author Martin Letenay
 */
public class StatusAndInfoCommand extends StatusCommand {
	
	private InformedStatus[] informedStatuses;
	private ISVNLocalResource svnResource;

    public StatusAndInfoCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll, boolean contactServer) {
        super(svnResource.getFile(), descend, getAll, contactServer);
        this.svnResource = svnResource;        
    }

    public void execute(ISVNClientAdapter client) throws SVNClientException {
        super.execute(client);
        
        informedStatuses = collectInformedStatuses(getStatuses());
        fetchNodeKinds(client, collectUnknownKinds(informedStatuses));
    }

    /**
     * Answer sorted array of informed statuses ... 
     * @return
     */
    public InformedStatus[] getInformedStatuses()
    {
    	return informedStatuses;
    }
    
    /**
     * Fetch nodeKind info for all InformedStatuses
     * 
     * @param client svnAdapter to run "list" command on
     * @param statuses - list of InformedStatuses which nodeKinds we want to get and set
     * @throws SVNClientException
     */
    private void fetchNodeKinds(ISVNClientAdapter client, InformedStatus[] statuses) throws SVNClientException
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
     * @param client svnAdapter to run "list" command on
     * @param statuse - InformedStatuses which nodeKinds we want to get and set
     * @throws SVNClientException
     */
    private void fetchNodeKind(ISVNClientAdapter client, InformedStatus status) throws SVNClientException
    {
    	SVNUrl url = SVNUrlUtils.getUrlFromLocalFileName(status.getPath(), svnResource.getUrl(), svnResource.getFile().getAbsolutePath());
    	ISVNInfo info = client.getInfo(url);
    	if (info != null)
    	{
    		status.setInformedKind(info.getNodeKind());
    	}
    }    

    private InformedStatus[] collectInformedStatuses(ISVNStatus[] statuses)
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
            int resourceType = SVNWorkspaceRoot.getResourceType(status.getPath());
            InformedStatus informedStatus = new InformedStatus(status);
            if ( SVNNodeKind.UNKNOWN  == status.getNodeKind() ) 
            {
                if( resourceType != resourceType)
                {
                	if (IResource.FILE == resourceType)
                	{
                		informedStatus.setInformedKind(SVNNodeKind.FILE);
                	}
                	else if(IResource.FOLDER == resourceType)
                	{
                		informedStatus.setInformedKind(SVNNodeKind.DIR);                		
                	}
                }
                else
                {
                	// check whether the resource path was not already added as parent of a previous resource.
                	// if it was then it is pretty clear that it is directory ...
                	if( containerSet.contains( status.getPath() ) )
                	{
                		informedStatus.setInformedKind(SVNNodeKind.DIR);                		
                	}
                }
            }
            containerSet.add(status.getPath().substring(0,status.getPath().lastIndexOf('/')));                    
            allStatuses.add(informedStatus);
        }
        
        InformedStatus[] result = new InformedStatus[allStatuses.size()];
        int i= 0;
        //In reverse-reverse order so dir syncInfos are created sooner then files ...
        for (ListIterator iter = allStatuses.listIterator(allStatuses.size()); iter.hasPrevious(); i++) {
        	result[i] = (InformedStatus) iter.previous();
		}
        
        return result;
    }
    
    private InformedStatus[] collectUnknownKinds(InformedStatus[] informedStatuses)
    {
    	List unknowns = new ArrayList();
    	for (int i = 0; i < informedStatuses.length; i++) {
			if (SVNNodeKind.UNKNOWN == informedStatuses[i].getNodeKind())
			{
				unknowns.add(informedStatuses[i]);
			}
		}
    	return (InformedStatus[]) unknowns.toArray(new InformedStatus[0]);
    }

    /**
     * A simple wrapper class for ISVNStatus + nodeKind.
     * Since we want to add/modify the nodeKind and ISVNStatuses are readonly ...
     * And we could also wrap an ResourceInfo eventually.
     *  
     */
    public static class InformedStatus implements ISVNStatus
	{
    	private SVNNodeKind informedKind;
    	private ISVNStatus realStatus;
    	
		protected InformedStatus(ISVNStatus realStatus) {
			super();
			this.informedKind = realStatus.getNodeKind();
			this.realStatus = realStatus;
		}
		
		public void setInformedKind(SVNNodeKind informedKind) {
			this.informedKind = informedKind;
		}
		
		public String toString()
		{
			return realStatus.toString() + " " + getNodeKind().toString();
		}
		
		/**
		 * Construct a resource from the status. 
		 * @return
		 */
		public IResource getResource()
		{
			IResource result = null;
			if (SVNNodeKind.DIR == getNodeKind())
			{
				result = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(getPath()));
			}
			else //SVNNodeKind.DIR or SVNNodeKind.UNKNOWN
			{
				result = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(getPath()));
			}
			return result;
		}
		
		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getUrl()
		 */
		public SVNUrl getUrl() {
			return realStatus.getUrl();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastChangedRevision()
		 */
		public Number getLastChangedRevision() {
			return realStatus.getLastChangedRevision();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastChangedDate()
		 */
		public Date getLastChangedDate() {
			return realStatus.getLastChangedDate();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastCommitAuthor()
		 */
		public String getLastCommitAuthor() {
			return realStatus.getLastCommitAuthor();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getTextStatus()
		 */
		public SVNStatusKind getTextStatus() {
			return realStatus.getTextStatus();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRepositoryTextStatus()
		 */
		public SVNStatusKind getRepositoryTextStatus() {
			return realStatus.getRepositoryTextStatus();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getPropStatus()
		 */
		public SVNStatusKind getPropStatus() {
			return realStatus.getPropStatus();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRepositoryPropStatus()
		 */
		public SVNStatusKind getRepositoryPropStatus() {
			return realStatus.getRepositoryPropStatus();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRevision()
		 */
		public Number getRevision() {
			return realStatus.getRevision();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getPath()
		 */
		public String getPath() {
			return realStatus.getPath();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getFile()
		 */
		public File getFile() {
			return realStatus.getFile();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getNodeKind()
		 */
		public SVNNodeKind getNodeKind() {
			return informedKind;
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#isCopied()
		 */
		public boolean isCopied() {
			return realStatus.isCopied();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getUrlCopiedFrom()
		 */
		public SVNUrl getUrlCopiedFrom() {
			return realStatus.getUrlCopiedFrom();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictNew()
		 */
		public File getConflictNew() {
			return realStatus.getConflictNew();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictOld()
		 */
		public File getConflictOld() {
			return realStatus.getConflictOld();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictWorking()
		 */
		public File getConflictWorking() {
			return realStatus.getConflictWorking();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLockOwner()
		 */
		public String getLockOwner() {
			return realStatus.getLockOwner();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLockCreationDate()
		 */
		public Date getLockCreationDate() {
			return realStatus.getLockCreationDate();
		}

		/* (non-Javadoc)
		 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLockComment()
		 */
		public String getLockComment() {
			return realStatus.getLockComment();
		}
    	
	}
}
