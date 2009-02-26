/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
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
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.ShowDifferencesAsUnifiedDiffDialogWC;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public class ShowDifferencesAsUnifiedDiffActionWC extends WorkbenchWindowAction {

	public ShowDifferencesAsUnifiedDiffActionWC() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
//		if(resources.length != 0) {
		if (resources.length > 0 && resources[0] instanceof IFile && !resources[0].isSynchronized(Depth.immediates)) {
			if (MessageDialog.openQuestion(getShell(), Policy.bind("DifferencesDialog.compare"), Policy.bind("CompareWithRemoteAction.fileChanged"))) {
				try {
					resources[0].refreshLocal(Depth.immediates, new NullProgressMonitor());
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}			
		}
			ShowDifferencesAsUnifiedDiffDialogWC dialog = new ShowDifferencesAsUnifiedDiffDialogWC(getShell(), resources[0], getTargetPart());
			if (dialog.open() == ShowDifferencesAsUnifiedDiffDialogWC.OK && !dialog.isDiffToOutputFile()) {
				try {
					if (resources[0] instanceof IContainer) {
						ISVNRemoteFolder remoteFolder = new RemoteFolder(dialog.getSvnResource().getRepository(), dialog.getToUrl(), dialog.getToRevision());
						CompareUI.openCompareEditorOnPage(
								new SVNLocalCompareInput(dialog.getSvnResource(), remoteFolder),
								getTargetPage());								
					} else {
						ISVNRemoteFile remoteFile = new RemoteFile(dialog.getSvnResource().getRepository(), dialog.getToUrl(), dialog.getToRevision());
						((RemoteFile)remoteFile).setPegRevision(dialog.getToRevision());
						CompareUI.openCompareEditorOnPage(
								new SVNLocalCompareInput(dialog.getSvnResource(), remoteFile),
								getTargetPage());
					}
				} catch (SVNException e) {
					MessageDialog.openError(getShell(), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.branchTag"), e.getMessage());						
				}				
			}
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedResources().length == 1;
	}
	
	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_COMPARE;
	}

}
