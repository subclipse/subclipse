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
package org.tigris.subversion.subclipse.ui.repository.model;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;

public class RemoteFolderElement extends SVNModelElement {

	/**
	 * Overridden to append the version name to remote folders which
	 * have version tags and are top-level folders.
	 */
	public String getLabel(Object o) {
		if (!(o instanceof ISVNRemoteFolder)) return null;
		ISVNRemoteFolder folder = (ISVNRemoteFolder)o;
		return folder.getName();
	}
	
	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof ISVNRemoteFolder)) return null;
		ISVNRemoteFolder folder = (ISVNRemoteFolder) object;
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}
	/**
	 * @see org.tigris.subversion.subclipse.ui.model.SVNModelElement#internalGetChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) throws TeamException {
		if (!(o instanceof ISVNRemoteFolder)) return new Object[0];
		return ((ISVNRemoteFolder)o).members(monitor);
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.model.SVNModelElement#isNeedsProgress()
	 */
	public boolean isNeedsProgress() {
		return true;
	}

    /**
     * Return null.
     */
    public Object getParent(Object o) {
        if (!(o instanceof ISVNRemoteFolder)) return null;
        ISVNRemoteFolder folder = (ISVNRemoteFolder)o;
        
        ISVNRemoteFolder parentFolder = folder.getParent();
        if (parentFolder != null)
            return parentFolder;
        else 
        {
            return folder.getRepository();
        }
    } 

}
