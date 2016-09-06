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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.OpenFileAction;

import com.collabnet.subversion.merge.AdaptableMergeResult;
import com.collabnet.subversion.merge.MergeResult;

public class OpenFileInSystemEditorAction extends OpenFileAction {
	private ISelectionProvider selectionProvider;

	public OpenFileInSystemEditorAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
		super(page);
		this.selectionProvider = selectionProvider;
	}
	
	protected List getSelectedResources() {
		ArrayList openableFiles = new ArrayList();
		IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			if (element instanceof AdaptableMergeResult) {
				MergeResult mergeResult = (MergeResult)element;
				if (mergeResult.getResource() instanceof IFile && !mergeResult.isDelete())
					openableFiles.add(mergeResult.getResource());
			}
		}
		return openableFiles;
	}

	protected List getSelectedNonResources() {		
		return Collections.EMPTY_LIST;
	}	
	
}
