/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.ResolveAction;
import org.tigris.subversion.subclipse.ui.conflicts.ResolveTreeConflictWizard;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeResultsFolder;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.views.MergeResultsView;
import com.collabnet.subversion.merge.wizards.DialogWizard;
import com.collabnet.subversion.merge.wizards.MergeWizardDialog;

public class MergeViewResolveAction extends ResolveAction {
	private IStructuredSelection fSelection;
	private boolean showDialog = true;
	private int selectedResolution;
	private SVNTreeConflict treeConflict;
	private File mergePath;

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		boolean treeConflictDialogShown = false;
		boolean compare = false;
		mergePath = null;
		if (showDialog) {
			List resources = new ArrayList();
			Iterator iter = fSelection.iterator();
			boolean folderSelected = false;
			boolean textConflicts = false;
			boolean propertyConflicts = false;
			boolean treeConflicts = false;
			while (iter.hasNext()) {
				MergeResult mergeResult = null;
				Object selectedObject = iter.next();
				if (selectedObject instanceof MergeResult) mergeResult = (MergeResult)selectedObject;
				if (selectedObject instanceof MergeResultsFolder) {
					folderSelected = true;
					MergeResultsFolder mergeResultsFolder = (MergeResultsFolder)selectedObject;
					resources.add(mergeResultsFolder.getFolder());
					mergeResult = mergeResultsFolder.getMergeResult();
				}
				if (mergeResult != null && (mergeResult.hasTreeConflict() || mergeResult.isConflicted() || mergeResult.isPropertyConflicted())) {
					if (mergeResult.isConflicted()) textConflicts = true;
					if (mergeResult.isPropertyConflicted()) propertyConflicts = true;
					if (mergeResult.hasTreeConflict()) treeConflicts = true;
					if (!(selectedObject instanceof MergeResultsFolder)) {
						resources.add(mergeResult.getResource());
					}
				}
			}
			if (resources.size() > 1) {
				if (!MessageDialog.openConfirm(getShell(), Messages.MergeViewResolveAction_confirm, Messages.MergeViewResolveAction_confirmMultiple)) return;
				setResolution(ISVNConflictResolver.Choice.chooseMerged);										
			} else if (treeConflicts) {
				IResource resource = (IResource)resources.get(0);
				treeConflict = getTreeConflict(resource);
				if (treeConflict == null) {
					String message = Messages.MergeViewResolveAction_confirmTreeConflict + resource.getName() + "?"; //$NON-NLS-1$
					if (!MessageDialog.openConfirm(getShell(), Messages.MergeViewResolveAction_confirm, message)) return;
					setResolution(ISVNConflictResolver.Choice.chooseMerged);										
				} else {
					ResolveTreeConflictWizard wizard = new ResolveTreeConflictWizard(treeConflict, getTargetPart());
					WizardDialog dialog = new SizePersistedWizardDialog(Display.getDefault().getActiveShell(), wizard, "ResolveTreeConflict"); //$NON-NLS-1$
					if (dialog.open() != WizardDialog.OK) return;								
					treeConflictDialogShown = true;
					compare = wizard.isCompare();
					mergePath = wizard.getMergePath();
				}				
			} else if (folderSelected) {
				IResource resource = (IResource)resources.get(0);
				String message = Messages.MergeViewResolveAction_confirmProperty + resource.getFullPath() + "?"; //$NON-NLS-1$
				if (!MessageDialog.openConfirm(getShell(), Messages.MergeViewResolveAction_confirm, message)) return;
				selectedResolution = ISVNConflictResolver.Choice.chooseMerged;
				setResolution(selectedResolution);
			} else {
				IResource[] resourceArray = new IResource[resources.size()];
				resources.toArray(resourceArray);
				DialogWizard dialogResolveWizard = new DialogWizard(DialogWizard.MARK_RESOLVED);
				dialogResolveWizard.setResources(resourceArray);
				dialogResolveWizard.setTextConflicts(textConflicts);
				dialogResolveWizard.setPropertyConflicts(propertyConflicts);
				dialogResolveWizard.setTreeConflicts(treeConflicts);
				MergeWizardDialog resolveDialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogResolveWizard);
				if (resolveDialog.open() == MergeWizardDialog.CANCEL) return;	
				selectedResolution = dialogResolveWizard.getResolution();
				setResolution(selectedResolution);
			}
		}

		if (!treeConflictDialogShown) super.execute(action);
		ArrayList mergeOutputs = new ArrayList();
		Iterator iter = fSelection.iterator();
		while (iter.hasNext()) {
			MergeResult mergeResult = null;
			Object selectedObject = iter.next();
			if (selectedObject instanceof MergeResult) mergeResult = (MergeResult)selectedObject;
			if (selectedObject instanceof MergeResultsFolder) {
				MergeResultsFolder mergeResultsFolder = (MergeResultsFolder)selectedObject;
				mergeResult = mergeResultsFolder.getMergeResult();
			}
			if (mergeResult != null && (mergeResult.hasTreeConflict() || mergeResult.isConflicted() || mergeResult.isPropertyConflicted())) {
				if (!compare) {
					String conflictResolution = Integer.toString(selectedResolution);
					mergeResult.setConflictResolution(conflictResolution);
					mergeResult.setPropertyResolution(conflictResolution);
					mergeResult.setTreeConflictResolution(conflictResolution);
					if (mergePath != null) {
						MergeResult[] allResults = mergeResult.getMergeOutput().getMergeResults();
						for (MergeResult checkResult : allResults) {
							if (checkResult.getResource().getLocation().toFile().equals(mergePath)) {
								try {
									LocalResourceStatus status = SVNProviderPlugin.getPlugin().getStatusCacheManager().getStatus(checkResult.getResource());
									if (status.isTextConflicted()) {
										checkResult.setAction(MergeResult.ACTION_CONFLICT);
										checkResult.setConflictResolution(" ");
										checkResult.setError(true);
									}
								} catch (SVNException e) {}
								break;
							}
						}
					}
				}
				if (!mergeOutputs.contains(mergeResult.getMergeOutput()))
					mergeOutputs.add(mergeResult.getMergeOutput());
			}
		}
		iter = mergeOutputs.iterator();
		while (iter.hasNext()) {
			MergeOutput mergeOutput = (MergeOutput)iter.next();
			mergeOutput.store();			
		}

		MergeResultsView.getView().refresh();
		iter = mergeOutputs.iterator();
		while(iter.hasNext()) {
			MergeOutput mergeOutput = (MergeOutput)iter.next();
			if (!mergeOutput.hasUnresolvedConflicts()) {
				DialogWizard dialogWizard = new DialogWizard(DialogWizard.CONFLICTS_RESOLVED);
				dialogWizard.setMergeOutput(mergeOutput);
				MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard, true);
				if (dialog.open() != MergeWizardDialog.CANCEL) MergeResultsView.getView().refresh(); 
			}
		}
	}

	public void selectionChanged(IAction action, ISelection sel) {
		super.selectionChanged(action, sel);
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
		try {
			action.setEnabled(isEnabled());
		} catch (Exception e) {}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean isEnabled() throws TeamException {
		boolean enabled = super.isEnabled();
		if (enabled) {
			Iterator iter = fSelection.iterator();
			while (iter.hasNext()) {
				Object selectedObject = iter.next();
				if (selectedObject instanceof MergeResult) {
					MergeResult mergeResult = (MergeResult)selectedObject;
					if (mergeResult.isResolved() && mergeResult.isPropertyResolved() && mergeResult.isTreeConflictResolved()) {
						enabled = false;
						break;
					}
				}
			}
		}
		return enabled;
	}

	@Override
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}

	public void setShowDialog(boolean showDialog) {
		this.showDialog = showDialog;
	}
	
	private SVNTreeConflict getTreeConflict(final IResource resource) {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				ISVNClientAdapter client = null;
				try {
					client = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getSVNClient();
					ISVNStatus[] statuses = client.getStatus(resource.getLocation().toFile(), true, true, true);
					for (int i = 0; i < statuses.length; i++) {
						if (statuses[i].hasTreeConflict()) {
							treeConflict = new SVNTreeConflict(statuses[i]);
							break;
						}
					}
				} catch (Exception e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				finally {
					SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().returnSVNClient(client);
				}
			}			
		});
		return treeConflict;
	}

}
