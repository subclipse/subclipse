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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;

/**
 * AdaptorFactory used to translate IResource in ISVNLocalResource if possible
 */
public class LocalResourceAdapterFactory implements IAdapterFactory {

	private static Class[] SUPPORTED_TYPES = new Class[] { ISVNLocalResource.class, ISVNLocalFile.class, ISVNLocalFolder.class};

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!(adaptableObject instanceof IResource)) {
			adaptableObject = ((IAdaptable)adaptableObject).getAdapter(IResource.class);
		}
		
		if (adaptableObject instanceof IResource) {
			IResource resource = (IResource)adaptableObject; 
			if (ISVNLocalResource.class.equals(adapterType)) {
				return SVNWorkspaceRoot.getSVNResourceFor(resource);							
			}
			if ((ISVNLocalFile.class.equals(adapterType)) && (adaptableObject instanceof IFile)) {
				IFile file = (IFile)resource;
				return SVNWorkspaceRoot.getSVNFileFor(file);
			}
			if ((ISVNLocalFolder.class.equals(adapterType)) && (adaptableObject instanceof IContainer)) {
				IContainer container = (IContainer)resource;
				return SVNWorkspaceRoot.getSVNFolderFor(container);
			}			
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	public Class[] getAdapterList() {
		return SUPPORTED_TYPES;
	}

}
