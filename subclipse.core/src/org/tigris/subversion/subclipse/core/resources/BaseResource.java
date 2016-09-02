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
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.SVNUrlUtils;

/**
 * Represents handles to SVN resource on the base (pristine copy).
 * Synchronization information is taken from the .svn subdirectories. 
 *
 * @see BaseFolder
 * @see BaseFile
 */
public abstract class BaseResource extends PlatformObject implements ISVNRemoteResource {

	private String charset = null;
	protected LocalResourceStatus localResourceStatus;
	protected IResource resource;
	private ISVNRemoteFolder parent;

	/**
	 * Constructor for BaseResource.
	 * @param localResourceStatus
	 */
	public BaseResource(IResource resource, LocalResourceStatus localResourceStatus)
	{
		Assert.isNotNull(resource);
		Assert.isNotNull(localResourceStatus);
		this.localResourceStatus = localResourceStatus;
		this.resource = resource;
	}

	/**
	 * Constructor for BaseResource.
	 * @param localResourceStatus
	 * @param charset
	 */
	public BaseResource(IResource resource, LocalResourceStatus localResourceStatus, String charset)
	{
		Assert.isNotNull(resource);
		Assert.isNotNull(localResourceStatus);
		this.localResourceStatus = localResourceStatus;
		this.charset = charset;
		this.resource = resource;
	}

	/**
	 * Create a BaseFile or BaseFolder according to nodeKind of the given status.
	 * @param localResourceStatus
	 * @return newly constructed BaseFile or BaseFolder instance
	 */
	public static BaseResource from(IResource resource, LocalResourceStatus localResourceStatus)
	{
		if (SVNNodeKind.FILE.equals(localResourceStatus.getNodeKind()))
		{
			return new BaseFile(resource, localResourceStatus);
		}
		else
		{
			return new BaseFolder(resource, localResourceStatus);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariant#getName()
	 */
	public String getName() {
		return localResourceStatus.getFile().getName();
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
     * @return a file corresponding to base resource
     */
    public File getFile()
    {
    	return localResourceStatus.getFile();
    }

    /**
     * Get resource path
     * @return a path corresponding to base resource
     */
    public IPath getPath()
    {
    	return localResourceStatus.getIPath();
    }    
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#getRepositoryRelativePath()
     */
    public String getRepositoryRelativePath() {
        return SVNUrlUtils.getRelativePath(getRepository().getUrl(), getUrl(), true);
    }    

    public String getProjectRelativePath() {
    	ISVNRemoteResource project = this;
    	while(project.getParent() != null) { project = project.getParent(); }
        return SVNUrlUtils.getRelativePath(project.getUrl(), getUrl(), true);
    }    

    public ISVNLogMessage[] getLogMessages(SVNRevision pegRevision,
			SVNRevision revisionStart, SVNRevision revisionEnd,
			boolean stopOnCopy, boolean fetchChangePath, long limit, boolean includeMergedRevisions)
			throws TeamException {
    	ISVNClientAdapter svnClient = getRepository().getSVNClient();
		try {
			return svnClient.getLogMessages(getFile(), pegRevision,
					revisionStart, revisionEnd, stopOnCopy, fetchChangePath,
					limit, includeMergedRevisions);
		} catch (SVNClientException e) {
			throw new TeamException("Failed in BaseResource.getLogMessages()",
					e);
		}
		finally {
			getRepository().returnSVNClient(svnClient);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
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
		return parent;
	}
	
	public void setParent(ISVNRemoteFolder parent) {
		this.parent = parent;
	}
	
	/**
	 * @return charset same as local resource.
	 */
	public String getCharset(){
		return charset;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return (localResourceStatus != null) ? localResourceStatus.getPath() : "";
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNResource#getResource()
	 */
	public IResource getResource() {
		return resource;
	}
}
