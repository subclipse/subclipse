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
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalBranchTagCompareInput;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.ShowDifferencesAsUnifiedDiffDialogWC;
import org.tigris.subversion.subclipse.ui.wizards.CheckoutWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardCompareMultipleResourcesWithBranchTagPage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public class ShowDifferencesAsUnifiedDiffActionWC extends WorkbenchWindowAction {

	public ShowDifferencesAsUnifiedDiffActionWC() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
		boolean refreshFile = false;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IFile && !resources[i].isSynchronized(Depth.immediates)) {
				if (refreshFile || MessageDialog.openQuestion(getShell(), Policy.bind("DifferencesDialog.compare"), Policy.bind("CompareWithRemoteAction.fileChanged"))) {
					refreshFile = true;
					try {
						resources[i].refreshLocal(Depth.immediates, new NullProgressMonitor());
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				} else {
					break;
				}
			}
		}

		if (resources.length > 1) {
			SvnWizardCompareMultipleResourcesWithBranchTagPage comparePage = new SvnWizardCompareMultipleResourcesWithBranchTagPage(resources);
			SvnWizard wizard = new SvnWizard(comparePage);
			SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
			if (dialog.open() == SvnWizardDialog.OK) {
				ISVNLocalResource[] localResources = new ISVNLocalResource[resources.length];
				for (int i = 0; i < resources.length; i++) {
					localResources[i] = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
				}
				try {
					SVNLocalBranchTagCompareInput compareInput = new SVNLocalBranchTagCompareInput(localResources, comparePage.getUrls(), comparePage.getRevision(), getTargetPart());
					CompareUI.openCompareEditorOnPage(
							compareInput,
							getTargetPage());					
				} catch (SVNException e) {
					MessageDialog.openError(getShell(), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.branchTag"), e.getMessage());
				}
			}
			return;
		}
		
		ShowDifferencesAsUnifiedDiffDialogWC dialog = new ShowDifferencesAsUnifiedDiffDialogWC(getShell(), resources[0], getTargetPart());
		if (dialog.open() == ShowDifferencesAsUnifiedDiffDialogWC.OK) {
			try {
				if (dialog.isDiffToOutputFile()) dialog.getOperation().run();
				if (!dialog.isDiffToOutputFile()) {
					SVNRevision pegRevision = dialog.getPegRevision();
					if (pegRevision == null) {
						pegRevision = SVNRevision.HEAD;
					}
					if (resources[0] instanceof IContainer) {
						ISVNRemoteFolder remoteFolder = new RemoteFolder(dialog.getSvnResource().getRepository(), dialog.getToUrl(), dialog.getToRevision());
						((RemoteFolder)remoteFolder).setPegRevision(pegRevision);
						SVNLocalCompareInput compareInput = new SVNLocalCompareInput(dialog.getSvnResource(), remoteFolder, pegRevision);
						compareInput.setDiffOperation(dialog.getOperation());
						CompareUI.openCompareEditorOnPage(
								compareInput,
								getTargetPage());						
					} else {
						ISVNRemoteFile remoteFile = new RemoteFile(dialog.getSvnResource().getRepository(), dialog.getToUrl(), dialog.getToRevision());
						((RemoteFile)remoteFile).setPegRevision(pegRevision);
						SVNLocalCompareInput compareInput = new SVNLocalCompareInput(dialog.getSvnResource(), remoteFile, pegRevision);
						CompareUI.openCompareEditorOnPage(
								compareInput,
								getTargetPage());
					}
				}
			} catch (SVNException e) {
				MessageDialog.openError(getShell(), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.branchTag"), e.getMessage());						
			}				
		}
	}

	protected boolean isEnabled() throws TeamException {
//		return getSelectedResources().length == 1;
		return true;
	}
	
	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_COMPARE;
	}

}
