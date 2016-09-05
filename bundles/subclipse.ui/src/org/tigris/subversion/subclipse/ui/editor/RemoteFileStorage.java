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
package org.tigris.subversion.subclipse.ui.editor;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class RemoteFileStorage extends PlatformObject implements IStorage {
	ISVNRemoteFile file;
	public RemoteFileStorage(ISVNRemoteFile file) {
		this.file = file;
	}

	/**
	 * Returns an open input stream on the contents of this file.
	 * The client is responsible for closing the stream when finished.
	 *
	 * @return an input stream containing the contents of the file
	 * @exception CoreException if this method fails. 
	 */
	public InputStream getContents() throws CoreException {
		try {
			final InputStream[] holder = new InputStream[1];
			SVNUIPlugin.runWithProgress(null, true /*cancelable*/, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						holder[0] = ((IResourceVariant)file).getStorage(monitor).getContents();
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}catch(CoreException e){
						throw new InvocationTargetException(e);
					}
				}
			});
			return holder[0];
		} catch (InterruptedException e) {
			// operation canceled
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof TeamException) {
				throw new CoreException(((TeamException) t).getStatus());
			}
			// should not get here
		}
		return new ByteArrayInputStream(new byte[0]);
	}
	public IPath getFullPath() {

        return null;
	}
	public String getName() {
		return file.getName();
	}
    
	public boolean isReadOnly() {
        // can't edit remote files
		return true;
	}
}

