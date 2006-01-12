/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.actions.CommitAction;

/**
 * Sync view operation for putting file system resources
 */
public class CommitSynchronizeOperation extends SVNSynchronizeOperation {
    private IResource[] resources;
    private boolean resourcesCommitted;

	protected CommitSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

    /* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#promptForConflictHandling(org.eclipse.swt.widgets.Shell, org.eclipse.team.core.synchronize.SyncInfoSet)
	 */
	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

    /* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#run(org.eclipse.team.examples.filesystem.FileSystemProvider, org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) {
		if (resourcesCommitted) return;
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				CommitAction commitAction = new CommitAction();
				commitAction.setSelectedResources(resources);
				commitAction.run(null);
			}			
		});
		resourcesCommitted = true;
	}

}
