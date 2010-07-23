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
 
import java.io.File;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalBaseCompareInput;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareSummaryInput;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperationWC;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public abstract class CompareWithRemoteAction extends WorkbenchWindowAction {

	private final SVNRevision revision;
	private boolean refresh;
	private boolean fileSelected;

	/**
	 * Creates a new CompareWithRemoteAction for the specified revision
	 * @param revision Revision to compare against.  Only relative resisions (HEAD,BASE,PREVIOUS) shoudl be used
	 */
	public CompareWithRemoteAction(SVNRevision revision) {
		this.revision = revision;
	}

	public void execute(IAction action) {
		refresh = false;
		fileSelected = false;
		final IResource[] resources = getSelectedResources();
		if (resources.length != 1 && !SVNRevision.BASE.equals(revision) && !SVNRevision.HEAD.equals(revision)) {
			return;
		}
		
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IFile) {
				fileSelected = true;
				if (!resources[i].isSynchronized(Depth.immediates)) {
					refresh = MessageDialog.openQuestion(getShell(), Policy.bind("DifferencesDialog.compare"), Policy.bind("CompareWithRemoteAction.fileChanged"));
					break;
				}
			}
		}
		
		try {
			final ISVNLocalResource[] localResources = new ISVNLocalResource[resources.length];
			for (int i = 0; i < resources.length; i++) {
				localResources[i] = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
			}
			
			final ISVNLocalResource localResource = localResources[0];
			
			run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						if (refresh) {
							for (int i = 0; i < localResources.length; i++) {
								if (resources[i] instanceof IFile) {
									localResources[i].getResource().refreshLocal(Depth.immediates, monitor);
								}
							}				
						}
						if (SVNRevision.BASE.equals(revision)) {
							if (localResources.length == 1 && localResource.getResource() instanceof IFile) {
								SVNLocalCompareInput compareInput = new SVNLocalCompareInput(localResource, revision);
								CompareUI.openCompareEditorOnPage(
										compareInput,
										getTargetPage());								
							} else {
								SVNLocalBaseCompareInput compareInput = new SVNLocalBaseCompareInput(localResources, revision);
								CompareUI.openCompareEditorOnPage(
										compareInput,
										getTargetPage());	
							}
						} else {					
							if (!fileSelected) {
								SVNLocalCompareSummaryInput compareInput = new SVNLocalCompareSummaryInput(localResources, revision);
								CompareUI.openCompareEditorOnPage(
										compareInput,
										getTargetPage());								
							} else {
								ISVNRemoteFile remoteFile = new RemoteFile(localResource.getRepository(), localResource.getUrl(), revision);
								((RemoteFile)remoteFile).setPegRevision(revision);
								SVNLocalCompareInput compareInput = new SVNLocalCompareInput(localResource, remoteFile);
								ShowDifferencesAsUnifiedDiffOperationWC operation = null;
								if (SVNRevision.HEAD.equals(revision)) {
									File file = File.createTempFile("revision", ".diff");
									file.deleteOnExit();
									operation = new ShowDifferencesAsUnifiedDiffOperationWC(getTargetPart(), localResource.getFile(), localResource.getUrl(), SVNRevision.HEAD, file);						
									operation.setGraphicalCompare(true);
									operation.run();
								}							
								compareInput.setDiffOperation(operation);
								CompareUI.openCompareEditorOnPage(
										compareInput,
										getTargetPage());
							}
						}
						
					} catch (Exception e) {
						handle(e, null, null);
					}
				}
			}, false /* cancelable */, PROGRESS_BUSYCURSOR);
		} catch (Exception e) {
			handle(e, null, null);
		}
		
		
	}
	
	/**
	 * Enable for resources that are managed (using super) or whose parent is an SVN folder.
	 * 
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) throws SVNException {
		if (svnResource.getResource() == null || !svnResource.getResource().exists()) {
			return false;
		}
		return super.isEnabledForSVNResource(svnResource) || svnResource.getParent().isManaged();
	}

	/**
	 * Added resources don't have any details to compare against
	 * TODO if the addition is because of a copy this should be allowed (requires some way to get the remote resource from the original location)
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForInaccessibleResources()
	 */
	protected boolean isEnabledForInaccessibleResources() {
        // it can be useful to compare the content of a file that has been deleted with the remote resource
        // this is particulary useful for CompareWithBaseRevisionAction
		return true;
	}
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		return SVNRevision.BASE.equals(revision) || SVNRevision.HEAD.equals(revision);
	}
	
	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_COMPARE;
	}
}
