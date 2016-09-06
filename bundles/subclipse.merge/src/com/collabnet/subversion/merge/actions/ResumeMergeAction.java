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

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionDelegate;

import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.wizards.DialogWizard;
import com.collabnet.subversion.merge.wizards.MergeWizardDialog;

public class ResumeMergeAction extends ActionDelegate {
	private IStructuredSelection fSelection;
	
	public void run(IAction action) {
		DialogWizard dialogWizard = new DialogWizard(DialogWizard.RESUME_MERGE);
		MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);		
		if (dialog.open() == MergeWizardDialog.CANCEL) return;
		Iterator iter = fSelection.iterator();
		while (iter.hasNext()) {
			Object selectedObject = iter.next();
			if (selectedObject instanceof MergeOutput) {
				MergeOutput mergeOutput = (MergeOutput)selectedObject;
				mergeOutput.resume();
			}
		}
	}

	public void selectionChanged(IAction action, ISelection sel) {
		boolean enabled = true;
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
			Iterator iter = fSelection.iterator();
			iterTag:
			while (iter.hasNext()) {
				Object selectedObject = iter.next();
				if (!(selectedObject instanceof MergeOutput)) {
					enabled = false;
					break;
				}
				MergeOutput mergeOutput = (MergeOutput)selectedObject;
				if (!mergeOutput.isIncomplete()) {
					enabled = false;
					break;
				}
				MergeResult[] conflictedMergeResults = mergeOutput.getConflictedMergeResults();
				for (int i = 0; i < conflictedMergeResults.length; i++) {
					if (!conflictedMergeResults[i].isResolved()) {
						enabled = false;
						break iterTag;
					}
				}
			}
		}
		action.setEnabled(enabled);
	}	
}
