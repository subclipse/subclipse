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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.history.GenericHistoryView;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.dialogs.DifferencesDialog;
import org.tigris.subversion.subclipse.ui.history.SVNHistoryPage;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ShowDifferencesAsUnifiedDiffAction extends WorkbenchWindowAction {
	private boolean usePegRevision;
	private SVNRevision pegRevision1;
	private SVNRevision pegRevision2;
//	
//	private IResource localResource;
//
//	public void setLocalResource(IResource localResource) {
//		this.localResource = localResource;
//	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		pegRevision1 = null;
		pegRevision2 = null;
		String fromRevision = null;
		String toRevision = null;
		ISVNResource[] selectedResources = getSelectedRemoteResources();
		SVNUrl fromUrl = null;
		SVNUrl toUrl = null;
		if (selectedResources == null || (selectedResources.length == 0)) {
			Object[] selectedObjects = selection.toArray();
			if (selectedObjects[0] instanceof ILogEntry) {
				selectedResources = new ISVNResource[2];
				selectedResources[0] = ((ILogEntry)selectedObjects[0]).getResource();
				fromRevision = ((ILogEntry)selectedObjects[0]).getRevision().toString();

				ILogEntry logEntry1 = (ILogEntry)selectedObjects[0];			
				RemoteResource remoteResource;
				
				IResource resource1 = logEntry1.getResource().getResource();
				if (resource1 != null) {
					try {
						ISVNRemoteResource baseResource = SVNWorkspaceRoot.getBaseResourceFor(resource1);
						if (baseResource != null) {
							pegRevision1 = baseResource.getLastChangedRevision();
						}
					} catch (Exception e) {}
				}
				
				if (logEntry1.getResource().getResource() instanceof IContainer) {
					remoteResource = new RemoteFolder(logEntry1.getResource().getRepository(), logEntry1.getResource().getUrl(), logEntry1.getRevision());
				}
				else {
					remoteResource = new RemoteFile(logEntry1.getResource().getRepository(), logEntry1.getResource().getUrl(), logEntry1.getRevision());
				}
				fromUrl = remoteResource.getUrl();
				
				if (selectedObjects.length > 1) {
					selectedResources[1] = ((ILogEntry)selectedObjects[1]).getResource();
					toRevision = ((ILogEntry)selectedObjects[1]).getRevision().toString();	
					
					ILogEntry logEntry2 = (ILogEntry)selectedObjects[1];
					
					IResource resource2 = logEntry2.getResource().getResource();
					if (resource2 != null) {
						try {
							ISVNRemoteResource baseResource = SVNWorkspaceRoot.getBaseResourceFor(resource2);
							if (baseResource != null) {
								pegRevision2 = baseResource.getLastChangedRevision();
							}
						} catch (Exception e) {}
					}					
					
					if (logEntry2.getResource().getResource() instanceof IContainer) {
						remoteResource = new RemoteFolder(logEntry2.getResource().getRepository(), logEntry2.getResource().getUrl(), logEntry2.getRevision());
					}
					else {
						remoteResource = new RemoteFile(logEntry2.getResource().getRepository(), logEntry2.getResource().getUrl(), logEntry2.getRevision());
					}

					toUrl = remoteResource.getUrl();
				}
				else {
					int from = Integer.parseInt(fromRevision);
					from--;
					toRevision = Integer.toString(from);
					toUrl = remoteResource.getUrl();
				}
			}
		} else {
			if (selectedResources[0] instanceof ISVNRemoteResource)
				fromRevision = ((ISVNRemoteResource)selectedResources[0]).getRevision().toString();
			if (selectedResources.length > 1 && selectedResources[1] instanceof ISVNRemoteResource)
				toRevision = ((ISVNRemoteResource)selectedResources[1]).getRevision().toString();			
		}
		if (pegRevision1 == null) {
			pegRevision1 = SVNRevision.HEAD;
		}
		if (pegRevision2 == null) {
			pegRevision2 = pegRevision1;
		}
		DifferencesDialog dialog = new DifferencesDialog(getShell(), null, selectedResources, new SVNRevision[] { pegRevision1, pegRevision2 }, getTargetPart());
		dialog.setUsePegRevision(usePegRevision);
		dialog.setFromUrl(fromUrl);
		dialog.setToUrl(toUrl);
		
		IResource localResource = null;
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (part instanceof GenericHistoryView) {
			IHistoryPage historyPage = ((GenericHistoryView)part).getHistoryPage();
			if (historyPage instanceof SVNHistoryPage) {
				localResource = ((SVNHistoryPage)historyPage).getResource();
			}
		}
		
		dialog.setLocalResource(localResource);
		if (!fromRevision.equals("HEAD")) dialog.setFromRevision(fromRevision); //$NON-NLS-1$
		if (toRevision != null && !toRevision.equals("HEAD")) dialog.setToRevision(toRevision); //$NON-NLS-1$  
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		Object[] selectedObjects = selection.toArray();
		if (selectedObjects.length == 0 || selectedObjects.length > 2) return false;
		ISVNResource svnResource1 = null;
		ISVNResource svnResource2 = null;
		if (selectedObjects[0] instanceof ISVNResource) svnResource1 = (ISVNResource)selectedObjects[0];
		else {
			if (selectedObjects[0] instanceof ILogEntry)
				svnResource1 = ((ILogEntry)selectedObjects[0]).getResource();
		}
		if (svnResource1 == null) return false;
		if (selectedObjects.length > 1) {
			if (selectedObjects[1] instanceof ISVNResource) svnResource2 = (ISVNResource)selectedObjects[1];
			else {
				if (selectedObjects[1] instanceof ILogEntry)
					svnResource2 = ((ILogEntry)selectedObjects[1]).getResource();		
			}
			if (!svnResource1.getRepository().getRepositoryRoot().toString().equals(svnResource2.getRepository().getRepositoryRoot().toString())) return false;
			return (svnResource1.isFolder() == svnResource2.isFolder());			
		}
		return true;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_DIFF;
	}

	public void setUsePegRevision(boolean usePegRevision) {
		this.usePegRevision = usePegRevision;
	}

}
