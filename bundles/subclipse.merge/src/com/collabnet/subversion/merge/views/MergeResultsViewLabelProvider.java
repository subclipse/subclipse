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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeResultsFolder;

public class MergeResultsViewLabelProvider extends LabelProvider implements IColorProvider {
	private IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
	private CompareConfiguration compareConfiguration = new CompareConfiguration();
	private MergeResultsDecorator mergeResultsDecorator = new MergeResultsDecorator();

	public Image getImage(Object element) {
		if (element instanceof MergeOutput) {
			Image image = null;
			if (((MergeOutput)element).isIncomplete())
				image = Activator.getImage(Activator.IMAGE_MERGE_OUTPUT_ABORTED);
			else {
				if (((MergeOutput)element).isAbnormalEnd()) image = Activator.getImage(Activator.IMAGE_MERGE_OUTPUT_ABNORMAL);
				else if (((MergeOutput)element).isInProgress()) image = Activator.getImage(Activator.IMAGE_MERGE_OUTPUT_IN_PROGRESS);
				else image = Activator.getImage(Activator.IMAGE_MERGE_OUTPUT);
			}
			return compareConfiguration.getImage(image, Differencer.NO_CHANGE);
		}
		if (element instanceof MergeResult) {
			MergeResult mergeResult = (MergeResult)element;
			if (mergeResult.getResource() == null || mergeResult.getResource().getName() == null) {
				return super.getImage(element);
			}
			return getImage(mergeResult);
		}
		if (element instanceof MergeResultsFolder) {
			MergeResultsFolder mergeResultsFolder = (MergeResultsFolder)element;
			Image image;
			if (mergeResultsFolder.getFolder().getFullPath().makeRelative().toString().length() > mergeResultsFolder.getRootFolderLength())
				image = workbenchLabelProvider.getImage(mergeResultsFolder.getFolder());
			else
				image = workbenchLabelProvider.getImage(mergeResultsFolder.getMergeOutput().getResource());
			MergeResult mergeResult = mergeResultsFolder.getMergeResult();
			if (mergeResult != null) return getImage(mergeResult);
			return compareConfiguration.getImage(image, Differencer.NO_CHANGE);
		}		
		return super.getImage(element);
	}
	
	private Image getImage(MergeResult mergeResult) {
		Image image = null;
		if (!mergeResult.getResource().exists() && mergeResult.getResource().getName().indexOf(".") == -1) { //$NON-NLS-1$
			image = workbenchLabelProvider.getImage(ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(mergeResult.getPath())));
		} else {
			image = workbenchLabelProvider.getImage(mergeResult.getResource());
		}

		if (mergeResult.getAction() != null && mergeResult.getAction().trim().length() > 0) {
			if (mergeResult.getAction().equals(MergeResult.ACTION_CHANGE)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.CHANGE);
			else if (mergeResult.getAction().equals(MergeResult.ACTION_ADD)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.ADD);
			else if (mergeResult.getAction().equals(MergeResult.ACTION_DELETE)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.DELETE);
			else if (mergeResult.getAction().equals(MergeResult.ACTION_CONFLICT)) {
				if (mergeResult.isResolved()) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.MERGE);
				else image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.CONFLICTED_CHANGE);
			}
			else if (mergeResult.getAction().equals(MergeResult.ACTION_MERGE)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.CHANGE);  
			else if (mergeResult.getAction().equals(MergeResult.ACTION_SKIP)) image = compareConfiguration.getImage(image, Differencer.NO_CHANGE);
		}
		
		if (mergeResult.getPropertyAction() != null && mergeResult.getPropertyAction().trim().length() > 0) {
			if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_CHANGE)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.PROPERTY_CHANGE);
			else if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_ADD)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.PROPERTY_ADD);
			else if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_DELETE)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.PROPERTY_DELETE);
			else if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_CONFLICT)) {
				if (mergeResult.isPropertyResolved()) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.MERGE);
				else image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.PROPERTY_CONFLICTED_CHANGE);
			}
			else if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_MERGE)) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.PROPERTY_CHANGE);  			
		}
		
		if (mergeResult.hasTreeConflict()) {
			if (mergeResult.isTreeConflictResolved()) image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.MERGE);
			else image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.TREE_CONFLICTED);
		}
		
		return image;
	}

	public String getText(Object element) {
		int mode = store.getInt(MergeResultsView.LAYOUT_PREFERENCE);
		if (element instanceof MergeOutput) {
			MergeOutput mergeOutput = (MergeOutput)element;
			if (mergeOutput.getDescription() == null) {
				if (mergeOutput.getResource() == null) return ""; //$NON-NLS-1$
				else return mergeOutput.getResource().getFullPath().makeRelative().toOSString();
			} else
				return mergeOutput.getDescription();
		}
		if (element instanceof MergeResultsFolder) {
			MergeResultsFolder folder = (MergeResultsFolder)element;
			return folder.toString();
		}
		if (element instanceof MergeResult) {
			MergeResult mergeResult = (MergeResult)element;
			if (mergeResult.getResource() != null && mergeResult.getResource().getName() != null) {
				if (mode == MergeResultsView.MODE_FLAT && mergeResult.getResource().getFullPath() != null) return mergeResult.getResource().getName() + " - " + mergeResult.getResource().getFullPath().toString(); //$NON-NLS-1$
				else return mergeResult.getResource().getName();
			}
		}
		return super.getText(element);
	}

	public Color getBackground(Object element) {
		return null;
	}

	public Color getForeground(Object element) {
		if (element instanceof MergeResult) {
			MergeResult mergeResult = (MergeResult)element;
			if (mergeResult.getAction().equals(MergeResult.ACTION_SKIP))
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
		}
		if (element instanceof MergeResultsFolder) {
			MergeResultsFolder mergeResultsFolder = (MergeResultsFolder)element;
			MergeResult mergeResult = mergeResultsFolder.getMergeResult();
			if (mergeResult != null && mergeResult.getAction().equals(MergeResult.ACTION_SKIP))
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
		}		
		return null;
	}

}
