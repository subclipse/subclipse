/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.compare;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ui.sync.SyncView;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.sync.SVNRemoteSyncElement;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.sync.SVNSyncCompareInput;

public class SVNLocalCompareEditorInput extends SVNSyncCompareInput {

	public SVNLocalCompareEditorInput(IResource[] resources) {
		super(resources);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#createDiffViewer(org.eclipse.swt.widgets.Composite)
	 */
	public Viewer createDiffViewer(Composite parent) {
		Viewer viewer = super.createDiffViewer(parent);
		getViewer().syncModeChanged(SyncView.SYNC_COMPARE);
		return viewer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.SyncCompareInput#createSyncElements(org.eclipse.core.runtime.IProgressMonitor)
	 */	
	protected IRemoteSyncElement[] createSyncElements(IProgressMonitor monitor) throws TeamException {
		IResource[] resources = getResources();
		IRemoteSyncElement[] trees = new IRemoteSyncElement[resources.length];
		int work = 100 * resources.length;
		monitor.beginTask(null, work);
		try {
			for (int i = 0; i < trees.length; i++) {
				IResource resource = resources[i];	

                IRemoteResource remote = SVNWorkspaceRoot.getLatestResourceFor(resource);
				trees[i] = new SVNRemoteSyncElement(false /* two-way */, resource, null, remote);				 
			}
		} finally {
			monitor.done();
		}
		//getViewer().resetFilters();
		return trees;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getTitle()
	 */
	public String getTitle() {
		StringBuffer title = new StringBuffer();
		IResource[] resources = getResources();
		for (int i = 0; i < resources.length; i++) {
			if (i != 0) {
				title.append(" - ");
			}
			title.append(resources[i].getName());
		}
		return Policy.bind("SVNLocalCompareEditorInput.title", title.toString()); //$NON-NLS-1$
	}

}
