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
import java.io.InputStream;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
/**
 * This class provides the implementation of ISVNRemoteFile 
 * for use by the repository and sync view.
 */
public class RemoteFile extends RemoteResource implements ISVNRemoteFile {
		
	/**
	 * Constructor (from byte[])
	 * @param local
	 * @param bytes
	 */
	public RemoteFile(IResource local, byte[] bytes) {
		super(local, bytes);
		
	}
	
	/**
	 * Constructor
	 * @param parent
	 * @param repository
	 * @param url
	 * @param revision
	 * @param lastChangedRevision
	 * @param date
	 * @param author
	 */
	public RemoteFile(
			RemoteFolder parent, 
			ISVNRepositoryLocation repository,
			SVNUrl url, 
			SVNRevision revision,
			SVNRevision.Number lastChangedRevision, 
			Date date, 
			String author) {
		super(parent, repository, url, revision, lastChangedRevision,
				date, author);
	}
	
	/**
	 * Constructor (from url and revision)
	 * @param repository
	 * @param url
	 * @param revision
	 */
	public RemoteFile(ISVNRepositoryLocation repository, SVNUrl url,
			SVNRevision revision) {
		super(repository, url, revision);
	}

	public RemoteFile(RemoteResourceStatus remoteStatusInfo)
	{
        this( null, 
        		remoteStatusInfo.getRepository(),
				remoteStatusInfo.getUrl(), 
				remoteStatusInfo.getRevision(),
				remoteStatusInfo.getLastChangedRevision(), 
				remoteStatusInfo.getLastChangedDate(), 
				remoteStatusInfo.getLastCommitAuthor());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#fetchContents(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void fetchContents(IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(Policy.bind("RemoteFile.getContents"), 100);//$NON-NLS-1$
		try {
			ISVNClientAdapter svnClient = repository.getSVNClient();
			InputStream inputStream;
			try {
				inputStream = svnClient.getContent(url, getRevision());
				super.setContents(inputStream, monitor);
			} catch (SVNClientException e) {
				throw new TeamException("Failed in RemoteFile.getContents()", e);
			}
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#members(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress){
		return new ISVNRemoteResource[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariant#isContainer()
	 */
	public boolean isContainer() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteFile))
			return false;
		RemoteFile remote = (RemoteFile) target;
		return super.equals(target)
				&& remote.getLastChangedRevision() == getLastChangedRevision();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteFile#getAnnotations(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNAnnotations getAnnotations(IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(Policy.bind("RemoteFile.getAnnotations"), 100);//$NON-NLS-1$
		try {
			ISVNClientAdapter svnClient = repository.getSVNClient();
			try {
				return svnClient.annotate(url, null, getRevision());
			} catch (SVNClientException e) {
				throw new TeamException(
						"Failed in remoteFile.getAnnotations()", e);
			}
		} finally {
			monitor.done();
		}
	}		
}
