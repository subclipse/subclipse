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
package org.tigris.subversion.subclipse.ui.repository.model;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.Policy;

public class RemoteFileElement extends SVNModelElement {
	/**
	 * Initial implementation: return null;
	 */
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
		return new Object[0];
	}
	/**
	 * Initial implementation: return null.
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof ISVNRemoteFile)) return null;
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(((ISVNRemoteFile)object).getName());
	}
	/**
	 * Initial implementation: return the file's name and version
	 */
	public String getLabel(Object o) {
		if (!(o instanceof ISVNRemoteFile)) return null;
		ISVNRemoteFile file = (ISVNRemoteFile)o;
		return Policy.bind("nameAndRevision", file.getName(),file.getLastChangedRevision().toString()); //$NON-NLS-1$
	}
    
    /**
     * Return null.
     */
    public Object getParent(Object o) {
        return null;
    }    
    
}
