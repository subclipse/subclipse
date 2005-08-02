/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Represents the base revision of a file 
 * 
 */
public class BaseFile extends RemoteFile {
	private LocalResourceStatus localResourceStatus;
	
	public BaseFile(LocalResourceStatus localResourceStatus)
	{
		super(null, 
				localResourceStatus.getRepository(), 
				localResourceStatus.getUrl(),
				SVNRevision.BASE, 
				localResourceStatus.getLastChangedRevision(), 
				localResourceStatus.getLastChangedDate(), 
				localResourceStatus.getLastCommitAuthor());
		Assert.isNotNull(localResourceStatus);
		this.localResourceStatus = localResourceStatus;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#fetchContents(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void fetchContents(IProgressMonitor monitor) throws TeamException {
		// we override the method because we can't get the content from the svnurl,
		// we need to use the underlying localResource

		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(Policy.bind("RemoteFile.getContents"), 100);//$NON-NLS-1$
		try {
			ISVNClientAdapter svnClient = repository.getSVNClient();
			InputStream inputStream;
			try {
				inputStream = svnClient.getContent(localResourceStatus.getFile(),
						getRevision());
				super.setContents(inputStream, monitor);
			} catch (SVNClientException e) {
				throw new TeamException("Failed in BaseFile.getContents()", e);
			}
		} finally {
			monitor.done();
		}
	}	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteFile#getAnnotations(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNAnnotations getAnnotations(IProgressMonitor monitor) throws TeamException {
		// we override the method because we can't get the content from the svnurl,
		// we need to use the underlying localResource
		
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(Policy.bind("RemoteFile.getAnnotations"), 100);//$NON-NLS-1$
		try {
			ISVNClientAdapter svnClient = repository.getSVNClient();
			InputStream inputStream;
			try {
				return svnClient.annotate(localResourceStatus.getFile(), null,
						getRevision());
			} catch (SVNClientException e) {
				throw new TeamException(
						"Failed in BaseFile.getAnnotations()", e);
			}
		} finally {
			monitor.done();
		}	
	}		
}
