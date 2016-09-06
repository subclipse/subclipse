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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionDelegate;

import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.views.MergeResultsView;

public class DeleteMergeOutputAction extends ActionDelegate {
	private IStructuredSelection fSelection;
	
	public void run(IAction action) {
		ArrayList deletions = new ArrayList();
		Iterator iter = fSelection.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof MergeOutput) {
				deletions.add(object);
			}
		}
		if (deletions.size() == 0) return;
		String message;
		if (deletions.size() == 1) {
			MergeOutput mergeOutput = (MergeOutput)deletions.get(0);
			message = Messages.DeleteMergeOutputAction_confirm + mergeOutput.getEditableValue() + "'?"; //$NON-NLS-1$
		} else {
			message = Messages.DeleteMergeOutputAction_confirmMultiple + deletions.size() + Messages.DeleteMergeOutputAction_confirmMultiple2;
		}
		if (!MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), Messages.DeleteMergeOutputAction_title, message)) return;
		iter = deletions.iterator();
		while (iter.hasNext()) {
			MergeOutput mergeOutput = (MergeOutput)iter.next();
			mergeOutput.delete();
		}
		if (MergeResultsView.getView() != null) MergeResultsView.getView().refresh();
	}

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	}	
}
