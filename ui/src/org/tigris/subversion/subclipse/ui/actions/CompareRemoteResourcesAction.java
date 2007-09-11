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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.ResourceEditionNode;
import org.tigris.subversion.subclipse.ui.compare.SVNCompareEditorInput;
import org.tigris.subversion.subclipse.ui.compare.SVNFolderCompareEditorInput;

/**
 * This action is used for comparing two arbitrary remote resources. This is
 * enabled in the repository explorer.
 */
public class CompareRemoteResourcesAction extends SVNAction {
	private ISVNRemoteResource[] remoteResources;

	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				ISVNRemoteResource[] editions = getSelectedRemoteResources();
				if (editions == null || editions.length != 2) {
					Object[] selectedObjects = selection.toArray();
					if (selectedObjects.length == 2 && selectedObjects[0] instanceof ILogEntry && selectedObjects[1] instanceof ILogEntry) {
						ILogEntry logEntry1 = (ILogEntry)selectedObjects[0];
						ILogEntry logEntry2 = (ILogEntry)selectedObjects[1];			
						try {
							ISVNRemoteFolder folder1 = new RemoteFolder(logEntry1.getResource().getRepository(), logEntry1.getResource().getUrl(), logEntry1.getRevision());
							ISVNRemoteFolder folder2 = new RemoteFolder(logEntry2.getResource().getRepository(), logEntry2.getResource().getUrl(), logEntry2.getRevision());
							compareFolders(folder1, folder2);							
						} catch (Exception e) {
							
						}
					} else {
						MessageDialog.openError(getShell(), Policy.bind("CompareRemoteResourcesAction.unableToCompare"), Policy.bind("CompareRemoteResourcesAction.selectTwoResources")); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return;
				}
				if (editions[0] instanceof ISVNRemoteFolder && editions[1] instanceof ISVNRemoteFolder) {
					compareFolders((ISVNRemoteFolder)editions[0], (ISVNRemoteFolder)editions[1]);
					return;
				}				
				ResourceEditionNode left = new ResourceEditionNode(editions[0]);
				ResourceEditionNode right = new ResourceEditionNode(editions[1]);
				CompareUI.openCompareEditorOnPage(
				  new SVNCompareEditorInput(left, right),
				  getTargetPage());
			}

			private void compareFolders(ISVNRemoteFolder folder1,
					ISVNRemoteFolder folder2) {
				CompareUI.openCompareEditorOnPage(
						  new SVNFolderCompareEditorInput(folder1, folder2),
						  getTargetPage());
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}
	
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
		ISVNRemoteResource[] resources = getSelectedRemoteResources();
		boolean enabled = (resources.length == 2) && (resources[0].isContainer() == resources[1].isContainer());
		if (!enabled) {
			Object[] selectedObjects = selection.toArray();
			if (selectedObjects.length == 2 && selectedObjects[0] instanceof ILogEntry && selectedObjects[1] instanceof ILogEntry) {
				ILogEntry logEntry1 = (ILogEntry)selectedObjects[0];
				ILogEntry logEntry2 = (ILogEntry)selectedObjects[1];
				if (logEntry1.getResource() != null && logEntry1.getResource().isFolder() && logEntry2.getResource() != null && logEntry2.getResource().isFolder())
					enabled = true;
			}
		}
		return enabled;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_COMPARE;
	}

	public void setRemoteResources(ISVNRemoteResource[] remoteResources) {
		this.remoteResources = remoteResources;
	}

	protected ISVNRemoteResource[] getSelectedRemoteResources() {
		if (remoteResources != null) return remoteResources;
		return super.getSelectedRemoteResources();
	}

}
