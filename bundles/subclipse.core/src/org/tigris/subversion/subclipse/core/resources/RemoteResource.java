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
package org.tigris.subversion.subclipse.core.resources;

import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.CachedResourceVariant;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.SVNUrlUtils;

/**
 * The purpose of this class and its subclasses is to implement the corresponding
 * ISVNRemoteResource interfaces for the purpose of communicating information about 
 * resources that reside in a SVN repository but have not necessarily been loaded
 * locally.
 */
public abstract class RemoteResource
	extends CachedResourceVariant
	implements ISVNRemoteResource {

	protected RemoteFolder parent;
	// null when this is the repository location 
	protected SVNUrl url;
	protected ISVNRepositoryLocation repository;
    protected SVNRevision revision;
    protected SVNRevision.Number lastChangedRevision;
    protected Date date;
    protected String author;
    protected SVNRevision pegRevision;

    /**
     * Constructor
     * @param local
     * @param bytes
     */
	public RemoteResource(IResource local, byte[] bytes){
		String nfo = new String(bytes);
		
		lastChangedRevision = new SVNRevision.Number(Long.parseLong(nfo));
		revision = lastChangedRevision;
		ISVNLocalResource res = SVNWorkspaceRoot.getSVNResourceFor(local);

		url = res.getUrl();
		repository = res.getRepository();
	}

	/**
	 * Constructor for RemoteResource.
	 * 
	 * @param parent
	 * @param repository
	 * @param url
	 * @param revision
	 * @param lastChangedRevision
	 * @param date
	 * @param author
	 */
	public RemoteResource(
		RemoteFolder parent,
		ISVNRepositoryLocation repository,
		SVNUrl url,
        SVNRevision revision,
		SVNRevision.Number lastChangedRevision,
		Date date,
		String author) {

		this.parent = parent;
		this.repository = repository;
		this.url = url;
        this.revision = revision;
        
		this.lastChangedRevision = lastChangedRevision;
		this.date = date;
		this.author = author;
	}

    /**
     * This constructor is used for the folder corresponding to repository location
     * @param repository
     * @param url
     * @param revision
     */
    public RemoteResource(ISVNRepositoryLocation repository, SVNUrl url, SVNRevision revision) {
        this.parent = null;
        this.repository = repository;
        this.url = url;
        this.revision = revision;
        
        // we don't know the following properties
        this.lastChangedRevision = null;
        this.date = null;
        this.author = null;
    }

	/*
	 * @see ISVNRemoteResource#getName()
	 */
	public String getName() {
		return (url != null) ? url.getLastPathSegment() : "";
	}

    /**
     * get the path of this remote resource relatively to the repository
     */
    public String getRepositoryRelativePath() {
        return SVNUrlUtils.getRelativePath(getRepository().getUrl(), getUrl(), true);
    }    
	
    public String getProjectRelativePath() {
    	ISVNRemoteResource project = this;
    	while(project.getParent() != null) { project = project.getParent(); }
        return SVNUrlUtils.getRelativePath(project.getUrl(), getUrl(), false);
    }    

    /*
	 * @see ISVNRemoteResource#exists(IProgressMonitor)
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException {
		
		return parent.exists(this, monitor);
	}
	
	/*
	 * @see ISVNRemoteResource#getParent()
	 */
	public ISVNRemoteFolder getParent() {
		return parent;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteResource))
			return false;
		RemoteResource remote = (RemoteResource) target;
		return remote.isContainer() == isContainer() && 
			remote.getUrl().equals(getUrl()) 
			&& remote.getRevision() == getRevision();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return getUrl().hashCode() + getRevision().hashCode();
	}

	public ISVNRepositoryLocation getRepository() {
		return repository;
	}

    /**
     * get the url of this remote resource
     */
    public SVNUrl getUrl() {
        return url;
    }

    /**
     * get the lastChangedRevision
     */
	public SVNRevision.Number getLastChangedRevision() {
		return lastChangedRevision;
	}

    /**
     * get the revision
     */
    public SVNRevision getRevision() {
        return revision;
    }

    /**
     * get the date 
     */
	public Date getDate() {
		return date;
	}

    /**
     * get the author
     */
	public String getAuthor() {
		return author;
	}
	
	public SVNRevision getPegRevision() {
		return pegRevision;
	}

	public void setPegRevision(SVNRevision pegRevision) {
		this.pegRevision = pegRevision;
	}	
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.team.core.variants.IResourceVariant#getContentIdentifier()
     */
    public String getContentIdentifier() {
        if (getLastChangedRevision() == null) {
        	if (revision == null) return "";
        	else return revision.toString();
        }
		return String.valueOf(getLastChangedRevision().getNumber());
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#getCachePath()
	 */
	protected String getCachePath() {
		return this.getUrl().toString() + ":" + getContentIdentifier();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#getCacheId()
	 */
	protected String getCacheId() {
		return SVNProviderPlugin.ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariant#asBytes()
	 */
	public byte[] asBytes() {
		return new Long(getContentIdentifier()).toString().getBytes();
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNResource#getResource()
     */
    public IResource getResource() {
    	return null;
    }
	
    public ISVNLogMessage[] getLogMessages(SVNRevision pegRevision,
			SVNRevision revisionStart, SVNRevision revisionEnd,
			boolean stopOnCopy, boolean fetchChangePath, long limit, boolean includeMergedRevisions)
			throws TeamException {
    	ISVNClientAdapter svnClient = repository.getSVNClient();
		try {
			return svnClient.getLogMessages(getUrl(),
					pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath,
					limit, includeMergedRevisions);
		} catch (SVNClientException e) {
			throw new TeamException("Failed in RemoteResource.getLogMessages()",
					e);
		}
		finally {
			repository.returnSVNClient(svnClient);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getCachePath();
	}

}
