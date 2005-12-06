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
 
import java.util.ArrayList;

import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

/**
 * Extension to the generic workbench content provider mechanism
 * to lazily determine whether an element has children.  That is,
 * children for an element aren't fetched until the user clicks
 * on the tree expansion box.
 */
public class RemoteContentProvider extends WorkbenchContentProvider {
	private boolean foldersOnly = false;
	
	/* (non-Javadoc)
	 * Method declared on WorkbenchContentProvider.
	 */
	public boolean hasChildren(Object element) {
		if (element == null) {
			return false;
		}
		
		if (element instanceof ISVNRepositoryLocation)
			return true;
		
		// the + box will always appear, but then disappear
		// if not needed after you first click on it.
		if (element instanceof ISVNRemoteResource) {
			return ((ISVNRemoteResource)element).isContainer();
		} 

		return super.hasChildren(element);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		IWorkbenchAdapter adapter = getAdapter(parentElement);
		if (adapter instanceof SVNModelElement) {
			Object[] children = ((SVNModelElement)adapter).getChildren(parentElement);
			if (foldersOnly) {
				ArrayList folderArray = new ArrayList();
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof ISVNRemoteFolder) folderArray.add(children[i]);
				}
				children = new Object[folderArray.size()];
				folderArray.toArray(children);
				return children;
			}
			else return children;
		}
		return super.getChildren(parentElement);
	}

	public void setFoldersOnly(boolean foldersOnly) {
		this.foldersOnly = foldersOnly;
	}

}
