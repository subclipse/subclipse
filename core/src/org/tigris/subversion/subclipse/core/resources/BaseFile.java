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
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * represents the base revision of a file 
 * 
 */
public class BaseFile extends RemoteFile implements ISVNRemoteFile {
	private ISVNLocalResource localResource;
	
	public BaseFile( 
			ISVNLocalResource localResource,
	        SVNRevision.Number lastChangedRevision,
	        Date date,
	        String author) throws SVNException {
		super(null, localResource.getRepository(), localResource.getUrl(), SVNRevision.BASE, lastChangedRevision, date, author);
		Assert.isNotNull(localResource);
		this.localResource = localResource;
	}	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.core.variants.CachedResourceVariant#fetchContents(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void fetchContents(IProgressMonitor monitor) throws TeamException {
		// we override method because we can't get the content from the svnurl,
		// we need to use the underlying localResource

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(Policy.bind("RemoteFile.getContents"), 100);//$NON-NLS-1$
		try {
			
				ISVNClientAdapter svnClient = repository.getSVNClient();
				InputStream inputStream;
				try {
					inputStream = svnClient.getContent(localResource.getFile(),
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
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.resources.RemoteFile#getAnnotations(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNAnnotations getAnnotations(IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(Policy.bind("RemoteFile.getAnnotations"), 100);//$NON-NLS-1$
		try {
			
				ISVNClientAdapter svnClient = repository.getSVNClient();
				InputStream inputStream;
				try {
					return svnClient.annotate(localResource.getFile(),null,getRevision()); 
				} catch (SVNClientException e) {
					throw new TeamException(
							"Failed in remoteFile.getAnnotations()", e);
				}
		} finally {
			monitor.done();
		}
		
	}	
	
}
