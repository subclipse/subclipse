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

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.history.Branches;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.Tags;

/**
 * Extension to the generic workbench content provider mechanism
 * to lazily determine whether an element has children.  That is,
 * children for an element aren't fetched until the user clicks
 * on the tree expansion box.
 */
public class RemoteContentProvider extends WorkbenchContentProvider {
	private boolean foldersOnly = false;
	private Branches branches;
	private Tags tags;
	private boolean includeBranchesAndTags = true;
	
	/* (non-Javadoc)
	 * Method declared on WorkbenchContentProvider.
	 */
	public boolean hasChildren(Object element) {
		if (element == null) {
			return false;
		}
		
		if (element instanceof Branches || element instanceof Tags) return true;
		if (element instanceof Alias) return false;
		
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
		if (parentElement instanceof Branches) return ((Branches)parentElement).getBranches();
		if (parentElement instanceof Tags) return ((Tags)parentElement).getTags();
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
			}
			if (parentElement instanceof ISVNRepositoryLocation && (branches != null || tags != null)) {
				ArrayList childrenArray = new ArrayList();
				for (int i = 0; i < children.length; i++) childrenArray.add(children[i]);
				if (branches != null) childrenArray.add(branches);
				if (tags != null) childrenArray.add(tags);
				children = new Object[childrenArray.size()];
				childrenArray.toArray(children);
			}
			return children;
		}
		return super.getChildren(parentElement);
	}

	public void setFoldersOnly(boolean foldersOnly) {
		this.foldersOnly = foldersOnly;
	}

	public void setResource(IResource resource) {
		if (includeBranchesAndTags) {
			AliasManager tagManager = new AliasManager(resource);
			Alias[] branchAliases = tagManager.getBranches();
			Alias[] tagAliases = tagManager.getTags();
			if (branchAliases.length > 0) branches = new Branches(branchAliases);
			if (tagAliases.length > 0) tags = new Tags(tagAliases);
		}
	}

	public void setIncludeBranchesAndTags(boolean includeBranchesAndTags) {
		this.includeBranchesAndTags = includeBranchesAndTags;
	}

}
