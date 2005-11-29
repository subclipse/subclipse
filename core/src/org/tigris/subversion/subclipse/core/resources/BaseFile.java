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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Represents the base revision of a file 
 * 
 */
public class BaseFile extends BaseResource implements ISVNRemoteFile {
	
	public BaseFile(LocalResourceStatus localResourceStatus)
	{
		super(localResourceStatus);
	}	

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
		return new BaseResourceStorage(this);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteResource#members(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNRemoteResource[] members(IProgressMonitor progress){
		return new ISVNRemoteResource[0];
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNRemoteFile#getAnnotations(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISVNAnnotations getAnnotations(IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(Policy.bind("RemoteFile.getAnnotations"), 100);//$NON-NLS-1$
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
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
