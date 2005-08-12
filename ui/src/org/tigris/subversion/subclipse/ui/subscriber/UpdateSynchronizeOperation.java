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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.core.sync.notused.SVNSyncInfo;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Sync view operation for getting file system resources
 */
public class UpdateSynchronizeOperation extends SVNSynchronizeOperation {

	protected UpdateSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#promptForConflictHandling(org.eclipse.swt.widgets.Shell, org.eclipse.team.core.synchronize.SyncInfoSet)
	 */
	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		// If there is a conflict in the syncSet, we need to prompt the user before proceeding.
		if (syncSet.hasConflicts() || syncSet.hasOutgoingChanges()) {
			switch (promptForConflicts(shell, syncSet)) {
			case 0:
				// Yes, synchronize conflicts as well
				break;
			case 1:
				// No, remove outgoing
				syncSet.removeConflictingNodes();
				syncSet.removeOutgoingNodes();
				break;
			case 2:
			default:
				// Cancel
				return false;
			}	
		}
		return true;
	}

	/**
	 * Prompts the user to determine how conflicting changes should be handled.
	 * Note: This method is designed to be overridden by test cases.
	 * @return 0 to sync conflicts, 1 to sync all non-conflicts, 2 to cancel
	 */
	private int promptForConflicts(Shell shell, SyncInfoSet syncSet) {
		String[] buttons = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
		String title = Policy.bind("SyncAction.update.conflict.title"); //$NON-NLS-1$
		String question = Policy.bind("SyncAction.update.conflict.question"); //$NON-NLS-1$
		final MessageDialog dialog = new MessageDialog(shell, title, null, question, MessageDialog.QUESTION, buttons, 0);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		return dialog.getReturnCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#run(org.eclipse.team.examples.filesystem.FileSystemProvider, org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor progress) throws InvocationTargetException, InterruptedException {
	    new UpdateOperation(getPart(), set.getResources(), getRepositoryRevision(set)).run(progress);
	}
	
	/**
	 * Get the revision number to which we want to update.
	 * @param set - syncInfoset of SyncInfos
	 * @return
	 */
	private SVNRevision getRepositoryRevision(SyncInfoSet set)
	{
		SyncInfo[] infos = set.getSyncInfos();
		if (infos.length > 0)
		{
			return ((SVNStatusSyncInfo) infos[0]).getRepositoryRevision();
		}
		else
		{
			return SVNRevision.HEAD;
		}
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
     */
    protected boolean canRunAsJob() {
        return true;
    }
}
