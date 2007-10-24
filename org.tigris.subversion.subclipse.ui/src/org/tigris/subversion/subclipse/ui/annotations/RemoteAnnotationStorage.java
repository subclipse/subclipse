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
package org.tigris.subversion.subclipse.ui.annotations;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class RemoteAnnotationStorage extends PlatformObject implements IEncodedStorage  {

	private InputStream contents;
	private ISVNRemoteFile file;
	
	public RemoteAnnotationStorage(ISVNRemoteFile file, InputStream contents) {
		this.file = file;
		this.contents = contents;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException {
		try {
			// Contents are a ByteArrayInputStream which can be reset to the beginning
			contents.reset();
		} catch (IOException e) {
			SVNUIPlugin.log(SVNException.wrapException(e));
		}
		return contents;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IEncodedStorage#getCharset()
	 */
	public String getCharset() throws CoreException {
		InputStream contents = getContents();
		try {
			String charSet = SVNUIPlugin.getCharset(getName(), contents);
			return charSet;
		} catch (IOException e) {
			throw new SVNException(new Status(IStatus.ERROR, SVNUIPlugin.ID, IResourceStatus.FAILED_DESCRIBING_CONTENTS, Policy.bind("RemoteAnnotationStorage.1", getFullPath().toString()), e)); //$NON-NLS-1$
		} finally {
			try {
				contents.close();
			} catch (IOException e1) {
				// Ignore
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		ISVNRepositoryLocation location = file.getRepository();
		SVNUrl repositoryUrl = location.getRepositoryRoot();
		String[] segments = repositoryUrl.getPathSegments();
		
		IPath path = new Path(null, "/");
		for (int i = 0; i < segments.length; i++) {
			path = path.append(segments[i]);
		}
		
		path = path.setDevice(repositoryUrl.getHost() + IPath.DEVICE_SEPARATOR);
		path = path.append(file.getRepositoryRelativePath());
		return path;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName() {
		return file.getName();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#isReadOnly()
	 */
	public boolean isReadOnly() {
		return true;
	}
}
