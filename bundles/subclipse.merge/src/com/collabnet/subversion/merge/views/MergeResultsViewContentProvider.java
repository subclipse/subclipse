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
package com.collabnet.subversion.merge.views;

import java.util.ArrayList;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.model.WorkbenchContentProvider;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeResultsFolder;

public class MergeResultsViewContentProvider extends WorkbenchContentProvider {
	private IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	
	public Object getParent(Object element) {
		return null;
	}
	
	public boolean hasChildren(Object element) {
		if (element instanceof MergeOutput || element instanceof MergeResultsFolder) return true;
		return false;
	}
	
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}
	
	public Object[] getChildren(Object parentElement) {
		int mode = store.getInt(MergeResultsView.LAYOUT_PREFERENCE);
		boolean conflictsOnly = store.getBoolean(MergeResultsView.CONFLICTS_ONLY_PREFERENCE);
		if (parentElement instanceof MergeResultsView) {
			return MergeOutput.getMergeOutputs();
		}
		if (parentElement instanceof MergeOutput) {
			MergeOutput mergeOutput = (MergeOutput)parentElement;
			if (mode == MergeResultsView.MODE_FLAT) {
				if (conflictsOnly) return mergeOutput.getConflictedMergeResults();
//				else return mergeOutput.getNonSkippedMergeResults();
				else return mergeOutput.getMergeResults();
			}
			if (mode == MergeResultsView.MODE_COMPRESSED_FOLDERS) {
				MergeResult[] rootMergeResults = mergeOutput.getRootMergeResults(conflictsOnly);
				MergeResultsFolder[] mergeResultFolders = mergeOutput.getCompressedFolders(conflictsOnly);
				ArrayList returnObjects = new ArrayList();
				for (int i = 0; i < mergeResultFolders.length; i++) 
					returnObjects.add(mergeResultFolders[i]);				
				for (int i = 0; i < rootMergeResults.length; i++)
					returnObjects.add(rootMergeResults[i]);
				Object[] returnArray = new Object[returnObjects.size()];
				returnObjects.toArray(returnArray);
				return returnArray;
			}
		}
		if (parentElement instanceof MergeResultsFolder) {
			MergeResultsFolder folder = (MergeResultsFolder)parentElement;
			if (folder.isCompressed()) {
				return folder.getMergeResults(conflictsOnly);
			}
		}
		return new Object[0];
	}
	
}
