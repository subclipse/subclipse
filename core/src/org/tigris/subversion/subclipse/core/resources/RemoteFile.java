/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;
import java.io.InputStream;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
 * This class provides the implementation of ISVNRemoteFile and IManagedFile for
 * use by the repository and sync view.
 */
public class RemoteFile extends RemoteResource implements ISVNRemoteFile {
	
	
	/**
	 * @param local
	 * @param bytes
	 */
	public RemoteFile(IResource local, byte[] bytes) {
		super(local, bytes);
		
	}
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
	public RemoteFile(ISVNRepositoryLocation repository, SVNUrl url,
			SVNRevision revision) {
		super(repository, url, revision);
	}
	/**
	 * @see ISVNRemoteFile#getContents()
	 */
	public void fetchContents(IProgressMonitor monitor) throws TeamException {
		// we cache the contents as getContents can be called several times
		// on the same RemoteFile
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(Policy.bind("RemoteFile.getContents"), 100);//$NON-NLS-1$
		try {
			
				ISVNClientAdapter svnClient = repository.getSVNClient();
				InputStream inputStream;
				try {
					inputStream = svnClient.getContent(url,
							getRevision());
					super.setContents(inputStream, monitor);
				} catch (SVNClientException e) {
					throw new TeamException(
							"Failed in remoteFile.getContents()", e);
				}
				monitor.done();
			
		} finally {
			monitor.done();
		}
	}
	/*
	 * @see IRemoteResource#members(IProgressMonitor)
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress){
		return new ISVNRemoteResource[0];
	}
	/*
	 * @see IRemoteResource#isContainer()
	 */
	public boolean isContainer() {
		return false;
	}
	/*
	 * @see ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteFile))
			return false;
		RemoteFile remote = (RemoteFile) target;
		return super.equals(target)
				&& remote.getLastChangedRevision() == getLastChangedRevision();
	}

	/**
	 * get annotations
	 * @param monitor
	 * @return
	 * @throws TeamException
	 */
	public ISVNAnnotations getAnnotations(IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(Policy.bind("RemoteFile.getAnnotations"), 100);//$NON-NLS-1$
		try {
			
				ISVNClientAdapter svnClient = repository.getSVNClient();
				InputStream inputStream;
				try {
					return svnClient.annotate(url,null,getRevision()); 
				} catch (SVNClientException e) {
					throw new TeamException(
							"Failed in remoteFile.getAnnotations()", e);
				}
		} finally {
			monitor.done();
		}
		
	}
	
	
}
