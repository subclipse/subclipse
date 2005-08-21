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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * IStorage implementation for accessing the contents of base resource
 *
 */
public class BaseResourceStorage extends PlatformObject implements IStorage {

	private BaseResource baseResource;
	
	public BaseResourceStorage(BaseResource baseResource)
	{
		super();
		this.baseResource = baseResource;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException {
		ISVNClientAdapter svnClient = baseResource.getRepository().getSVNClient();
		try {
			return svnClient.getContent(baseResource.getFile(), baseResource.getRevision());
		} catch (SVNClientException e) {
			throw new CoreException(new Status(IStatus.ERROR, SVNProviderPlugin.ID, 0, "Failed in BaseFile.getContents()", e)); //$NON-NLS-1$);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		return baseResource.getPath();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName() {
		return baseResource.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#isReadOnly()
	 */
	public boolean isReadOnly() {
		return true;
	}
}
