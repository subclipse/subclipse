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
import org.tigris.subversion.svnclientadapter.ISVNLock;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;


/**
 * This class provides the implementation of ISVNRemoteFile for use by the
 * repository and sync view.
 */
public class RemoteFile extends RemoteResource implements ISVNRemoteFile {
	private ISVNLock lock;
	private boolean cached = false;

	/**
	 * Constructor (from byte[])
	 * 
	 * @param local
	 * @param bytes
	 */
	public RemoteFile(IResource local, byte[] bytes) {
		super(local, bytes);

	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param repository
	 * @param url
	 * @param revision
	 * @param lastChangedRevision
	 * @param date
	 * @param author
	 */
	public RemoteFile(RemoteFolder parent, ISVNRepositoryLocation repository,
			SVNUrl url, SVNRevision revision,
			SVNRevision.Number lastChangedRevision, Date date, String author) {
		super(parent, repository, url, revision, lastChangedRevision, date,
				author);
	}

	/**
	 * Constructor (from url and revision)
	 * 
	 * @param repository
	 * @param url
	 * @param revision
	 */
	public RemoteFile(ISVNRepositoryLocation repository, SVNUrl url,
			SVNRevision revision) {
		super(repository, url, revision);
	}

	/**
	 * Constructor
	 * @param remoteStatusInfo
	 */
	public RemoteFile(RemoteResourceStatus remoteStatusInfo) {
		this(null, remoteStatusInfo.getRepository(), remoteStatusInfo.getUrl(),
				remoteStatusInfo.getRepositoryRevision(), remoteStatusInfo
						.getLastChangedRevision(), remoteStatusInfo
						.getLastChangedDate(), remoteStatusInfo
						.getLastCommitAuthor());
	}

	public ISVNLock getLock() {
		return lock;
	}

	public void setLock(ISVNLock lock) {
		this.lock = lock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#fetchContents(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void fetchContents(IProgressMonitor aMonitor) throws TeamException {
		IProgressMonitor monitor = Policy.monitorFor(aMonitor);
		monitor.beginTask(Policy.bind("RemoteFile.getContents"), 100);//$NON-NLS-1$
		ISVNClientAdapter svnClient = null;
		try {
			svnClient = repository.getSVNClient();
			InputStream inputStream = null;
			try {
				
				if (pegRevision != null) {
					try {
						inputStream = svnClient.getContent(url, revision, pegRevision);
					}
					catch (SVNClientException e1) {}
				}
				if (inputStream == null) {
					inputStream = svnClient.getContent(url, getRevision(), getRevision());
				}
				super.setContents(inputStream, monitor);
				cached = true;
			} catch (SVNClientException e) {
				throw new TeamException("Failed in RemoteFile.getContents()", e);
			}
		} finally {
			repository.returnSVNClient(svnClient);
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#members(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress) {
		return new ISVNRemoteResource[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.IResourceVariant#isContainer()
	 */
	public boolean isContainer() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}

	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#isHandleCached()
	 */
	@Override
	protected boolean isHandleCached() {
		if (revision.equals(SVNRevision.HEAD)) {
			if (!cached) {
				return false;
			}
		}
		return super.isHandleCached();
	}

	/*
	 * (non-Javadoc)
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteFile#getAnnotations(org.tigris.subversion.svnclientadapter.SVNRevision,
	 *      org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public ISVNAnnotations getAnnotations(SVNRevision fromRevision,
			SVNRevision toRevision, boolean includeMergedRevisions, boolean ignoreMimeType) throws TeamException {
		ISVNClientAdapter svnClient = repository.getSVNClient();
		try {
			return svnClient.annotate(url, fromRevision,
					toRevision, pegRevision, ignoreMimeType, includeMergedRevisions);
		} catch (SVNClientException e) {
			throw new TeamException("Failed in remoteFile.getAnnotations()", e);
		}
		finally {
			repository.returnSVNClient(svnClient);
		}
	}
}
