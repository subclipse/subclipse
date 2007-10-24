/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Represents the base revision of a file.
 * 
 */
public class BaseFile extends BaseResource implements ISVNRemoteFile {
	
	/**
	 * Constructor
	 * @param localResourceStatus
	 */
	public BaseFile(LocalResourceStatus localResourceStatus)
	{
		super(localResourceStatus);
	}	

	/**
	 * Constructor
	 * @param localResourceStatus
	 * @param charset
	 */
	public BaseFile(LocalResourceStatus localResourceStatus, String charset) {
		super(localResourceStatus, charset);
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
	 * @see org.eclipse.team.core.variants.IResourceVariant#getStorage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException
	{
		return BaseResourceStorageFactory.current().createResourceStorage(this);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#members(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress){
		return new ISVNRemoteResource[0];
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteFile#getAnnotations(org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public ISVNAnnotations getAnnotations(SVNRevision fromRevision,
			SVNRevision toRevision, boolean includeMergedRevisions) throws TeamException {
		try {
			boolean ignoreMimeType = false; //hard-coded for now
			return getRepository().getSVNClient().annotate(
					localResourceStatus.getFile(), fromRevision, toRevision, ignoreMimeType, includeMergedRevisions);
		} catch (SVNClientException e) {
			throw new TeamException("Failed in BaseFile.getAnnotations()", e);
		}
	}
}
