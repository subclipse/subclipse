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
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.SaveablePartDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.SVNCompareRevisionsInput;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.utils.Depth;

/**
 * Used when you want to compare local resource with remote ones 
 */
public class CompareWithRevisionAction extends WorkbenchWindowAction {
	private boolean refresh;
	
	/**
	 * Returns the selected remote file
	 */
	protected ISVNRemoteFile getSelectedRemoteFile() {
		IResource[] resources = getSelectedResources();
		if (resources.length != 1) return null;
		if (!(resources[0] instanceof IFile)) return null;
		IFile file = (IFile)resources[0];
		try {
			return (ISVNRemoteFile)SVNWorkspaceRoot.getBaseResourceFor(file);
		} catch (TeamException e) {
			handle(e, null, null);
			return null;
		}
	}

	/*
	 * @see SVNAction#execute(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		refresh = false;
		
		// Setup holders
		final ISVNRemoteFile[] file = new ISVNRemoteFile[] { null };
		final ILogEntry[][] entries = new ILogEntry[][] { null };
		
		// Get the selected file
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				file[0] = getSelectedRemoteFile();
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
		
		if (file[0] == null) {
			// No revisions for selected file
			MessageDialog.openWarning(getShell(), Policy.bind("CompareWithRevisionAction.noRevisions"), Policy.bind("CompareWithRevisionAction.noRevisionsLong")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		
		if (!file[0].getResource().isSynchronized(Depth.immediates)) {
			refresh = MessageDialog.openQuestion(getShell(), Policy.bind("DifferencesDialog.compare"), Policy.bind("CompareWithRemoteAction.fileChanged"));
		}
		
		// Fetch the log entries
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					monitor.beginTask(Policy.bind("CompareWithRevisionAction.fetching"), 100); //$NON-NLS-1$
					AliasManager tagManager = null;
					IResource[] resources = getSelectedResources();
					if (refresh) resources[0].refreshLocal(Depth.immediates, monitor);
					if (resources.length == 1) tagManager = new AliasManager(resources[0]);
					GetLogsCommand logCmd = new GetLogsCommand(file[0], SVNRevision.HEAD, SVNRevision.HEAD, new SVNRevision.Number(0), false, 0, tagManager, false);
					logCmd.run(Policy.subMonitorFor(monitor, 100));
					entries[0] = logCmd.getLogEntries(); 					
					monitor.done();
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		}, true /* cancelable */, PROGRESS_DIALOG);
		
		if (entries[0] == null) return;
		
		// Show the compare viewer
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				SVNCompareRevisionsInput input = new SVNCompareRevisionsInput((IFile)getSelectedResources()[0], entries[0]);
				if(SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG)) {
					// running with a null progress monitor is fine because we have already pre-fetched the log entries above.
					input.run(new NullProgressMonitor());
					SaveablePartDialog cd = createCompareDialog(getShell(), input);
					cd.setBlockOnOpen(true);
					cd.open();
				} else {
					CompareUI.openCompareEditorOnPage(input, getTargetPage());
				}
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}
	
	/**
	 * Return the compare dialog to use to show the compare input.
	 */
	protected SaveablePartDialog createCompareDialog(Shell shell, SVNCompareRevisionsInput input) {
		return new SaveablePartDialog(shell, input); //$NON-NLS-1$
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("CompareWithRevisionAction.compare"); //$NON-NLS-1$
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNResource)
	 */
	protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) throws SVNException {
		return (!svnResource.isFolder() && super.isEnabledForSVNResource(svnResource));
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		return false;
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_COMPARE;
	}
}
