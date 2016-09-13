/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ExternalFileStorage implements IStorage {

	private IFileStore fFileStore;
	private IPath fFullPath;
	
	public ExternalFileStorage(IFileStore fileStore) {
		fFileStore= fileStore;
	}

	public InputStream getContents() throws CoreException {
		return fFileStore.openInputStream(EFS.NONE, null);
	}

	public IPath getFullPath() {
    	if (fFullPath == null)
    		fFullPath= new Path(fFileStore.toURI().getPath());
    	return fFullPath;
	}

	public String getName() {
		return fFileStore.getName();
	}

	public boolean isReadOnly() {
		return fFileStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY);
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
}
