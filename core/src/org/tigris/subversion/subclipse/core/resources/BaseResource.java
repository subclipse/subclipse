/* ***************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *
 * ***************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;
import java.util.Date;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNUrlUtils;

/**
 * Represents handles to SVN resource on the base (pristine copy).
 * Synchronization information is taken from the .svn subdirectories. 
 *
 * @see BaseFolder
 * @see BaselFile
 */
public abstract class BaseResource extends PlatformObject implements ISVNRemoteResource {

	protected LocalResourceStatus localResourceStatus;

	/**
	 * Constructor for BaseResource.
	 */
	public BaseResource(LocalResourceStatus localResourceStatus)
	{
		Assert.isNotNull(localResourceStatus);
		this.localResourceStatus = localResourceStatus;		
	}

	/**
	 * Create a BaseFile or BaseFolder according to nodeKind of the given status.
	 * @param localResourceStatus
	 * @return
	 */
	public static BaseResource from(LocalResourceStatus localResourceStatus)
	{
		if (SVNNodeKind.FILE.equals(localResourceStatus.getNodeKind()))
		{
			return new BaseFile(localResourceStatus);
		}
		else
		{
			return new BaseFolder(localResourceStatus);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariant#getName()
	 */
	public String getName() {
		SVNUrl url = localResourceStatus.getUrl();
		return (url != null) ? url.getLastPathSegment() : "";
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.core.variants.IResourceVariant#getContentIdentifier()
     */
    public String getContentIdentifier() {
		return SVNRevision.BASE.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariant#asBytes()
	 */
	public byte[] asBytes() {
		return getContentIdentifier().getBytes();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof BaseResource))
			return false;
		BaseResource base = (BaseResource) target;
		return base.isContainer() == isContainer() && 
			base.getUrl().equals(getUrl()) 
			&& base.getRevision() == getRevision();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return getUrl().hashCode() + getRevision().hashCode();
	}
	
	public ISVNRepositoryLocation getRepository() {
		return localResourceStatus.getRepository();
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNResource#getUrl()
     */
    public SVNUrl getUrl() {
        return localResourceStatus.getUrl();
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getLastChangedRevision()
	 */
	public SVNRevision.Number getLastChangedRevision() {
		return localResourceStatus.getLastChangedRevision();
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getRevision()
     */
    public SVNRevision getRevision() {
        return SVNRevision.BASE;
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getDate()
	 */
	public Date getDate() {
		return localResourceStatus.getLastChangedDate();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getAuthor()
	 */
	public String getAuthor() {
		return localResourceStatus.getLastCommitAuthor();
	}

    /**
     * Get resource file
     * @return
     */
    public File getFile()
    {
    	return localResourceStatus.getFile();
    }

    /**
     * Get resource path
     * @return
     */
    public IPath getPath()
    {
    	return localResourceStatus.getPath();
    }    
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getRepositoryRelativePath()
     */
    public String getRepositoryRelativePath() {
        return SVNUrlUtils.getRelativePath(getRepository().getUrl(), getUrl(), true);
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getLogEntries(org.eclipse.core.runtime.IProgressMonitor)
     */
    public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws SVNException {
        GetLogsCommand command = new GetLogsCommand(this);
        command.run(monitor);
        return command.getLogEntries();
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getLogEntries(org.eclipse.core.runtime.IProgressMonitor, , SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit)
     */
    public ILogEntry[] getLogEntries(IProgressMonitor monitor, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit) throws SVNException {
        GetLogsCommand command = new GetLogsCommand(this);
        command.setPegRevision(pegRevision);
        command.setRevisionStart(revisionStart);
        command.setRevisionEnd(revisionEnd);
        command.setStopOnCopy(stopOnCopy);
        command.setLimit(limit);
        command.run(monitor);
        return command.getLogEntries();   
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#exists(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getParent()
	 */
	public ISVNRemoteFolder getParent() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return (localResourceStatus != null) ? localResourceStatus.getPathString() : "";
	}

}
