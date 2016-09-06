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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionDelegate;
import org.tigris.subversion.subclipse.ui.actions.RevertAction;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.UndoMergeOperation;
import com.collabnet.subversion.merge.views.MergeResultsView;
import com.collabnet.subversion.merge.wizards.DialogWizard;
import com.collabnet.subversion.merge.wizards.MergeWizardDialog;

public class UndoMergeAction extends ActionDelegate {
	private IStructuredSelection fSelection;
	
	public void run(IAction action) {
		DialogWizard dialogWizard = new DialogWizard(DialogWizard.UNDO_MERGE_WARNING);
		MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard, true);
		if (dialog.open() == MergeWizardDialog.CANCEL) return;
		final ArrayList resources = new ArrayList();
		ArrayList mergeOutputs = new ArrayList();
		Iterator iter = fSelection.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof MergeOutput) {
				MergeOutput mergeOutput = (MergeOutput)object;
				mergeOutputs.add(mergeOutput);
				IResource resource = mergeOutput.getResource();
				resources.add(resource);
			}
		}
		final IResource[] resourceArray = new IResource[resources.size()];
		resources.toArray(resourceArray);
		UndoMergeOperation undoMergeOperation = new UndoMergeOperation(MergeResultsView.getView(), resourceArray);
		try {
			undoMergeOperation.run();
		} catch (Exception e) {
			Activator.handleError(Messages.UndoMergeAction_error, e);
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UndoMergeAction_title, e.getMessage());
			return;
		}
		iter = mergeOutputs.iterator();
		while (iter.hasNext()) {
			MergeOutput mergeOutput = (MergeOutput)iter.next();
			mergeOutput.delete();
		}
		MergeResultsView.getView().refresh();
		dialogWizard = new DialogWizard(DialogWizard.UNDO_MERGE_COMPLETED);
		dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard, true);
		if (dialog.open() == MergeWizardDialog.CANCEL) return;
		RevertAction revertAction = new RevertAction();
		revertAction.setShowNothingToRevertMessage(false);
		IStructuredSelection selection = new IStructuredSelection() {
			public Object getFirstElement() {
				return resourceArray[0];
			}
			public Iterator iterator() {
				return toList().iterator();
			}	
			public int size() {
				return resourceArray.length;
			}	
			public Object[] toArray() {
				return resourceArray;
			}
			public List toList() {
				return resources;
			}
			public boolean isEmpty() {
				return resources.isEmpty();
			}		
		};
		revertAction.selectionChanged(null, selection);
		revertAction.run(null);					
	}

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	}	
}
