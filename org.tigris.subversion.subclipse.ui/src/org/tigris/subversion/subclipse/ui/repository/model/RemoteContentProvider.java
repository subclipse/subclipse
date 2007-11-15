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

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.Branches;
import org.tigris.subversion.subclipse.core.history.Tags;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;

/**
 * Extension to the generic workbench content provider mechanism
 * to lazily determine whether an element has children.  That is,
 * children for an element aren't fetched until the user clicks
 * on the tree expansion box.
 */
public class RemoteContentProvider extends WorkbenchContentProvider {
	private Branches branches;
	private Tags tags;
	private boolean includeBranchesAndTags = true;
	private RemoteFolder rootFolder;
	private boolean useDeferredContentManager = true;

	private DeferredTreeContentManager manager;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof AbstractTreeViewer) {
			manager = new DeferredTreeContentManager(this, (AbstractTreeViewer) viewer);
		}
		super.inputChanged(viewer, oldInput, newInput);
	}

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

		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}

		return super.hasChildren(element);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Branches) return ((Branches)parentElement).getBranches();
		if (parentElement instanceof Tags) return ((Tags)parentElement).getTags();
		if (useDeferredContentManager && manager != null) {
			Object[] children = manager.getChildren(parentElement);
			if (children != null) {
				if (parentElement instanceof ISVNRepositoryLocation && (rootFolder != null || branches != null || tags != null)) {
					ArrayList childrenArray = new ArrayList();
					if (rootFolder != null) childrenArray.add(rootFolder);
					if (branches != null) childrenArray.add(branches);
					if (tags != null) childrenArray.add(tags);
					for (int i = 0; i < children.length; i++) childrenArray.add(children[i]);

					children = new Object[childrenArray.size()];
					childrenArray.toArray(children);
				}
				// This will be a placeholder to indicate
				// that the real children are being fetched
				return children;
			}
		}
		return super.getChildren(parentElement);
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

	public void cancelJobs(ISVNRepositoryLocation[] roots) {
		if (manager != null) {
			for (int i = 0; i < roots.length; i++) {
				ISVNRepositoryLocation root = roots[i];
				cancelJobs(root);
			}
		}
	}

	/**
	 * Cancel any jobs that are fetching content from the given location.
	 * @param location
	 */
	public void cancelJobs(ISVNRepositoryLocation location) {
		if (manager != null) {
			manager.cancel(location);
		}
	}

	public void setRootFolder(RemoteFolder rootFolder) {
		this.rootFolder = rootFolder;
	}

	public void setUseDeferredContentManager(boolean useDeferredContentManager) {
		this.useDeferredContentManager = useDeferredContentManager;
	}
}
