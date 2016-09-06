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
package com.collabnet.subversion.merge.wizards;

import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeResultsFolder;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.views.MergeResultsDecorator;

public class MergeAbortedWizardPage extends WizardPage {
	private MergeOutput mergeOutput;
	private String errorMessage;
	private TreeViewer treeViewer;
	
	public MergeAbortedWizardPage(String pageName) {
		super(pageName, Messages.MergeAbortedWizardPage_title, Activator.getDefault().getImageDescriptor(Activator.IMAGE_SVN));
	}	

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);	
		
		Label label = new Label(composite, SWT.WRAP);
		label.setText(errorMessage);		
		data = new GridData();
		data.widthHint = 500;
		label.setLayoutData(data);
		
		new Label(composite, SWT.NONE);
		
		Group conflictGroup = new Group(composite, SWT.NULL);
		conflictGroup.setText(Messages.MergeAbortedWizardPage_conflicts);
		GridLayout conflictLayout = new GridLayout();
		conflictLayout.numColumns = 1;
		conflictGroup.setLayout(conflictLayout);
		data = new GridData(GridData.FILL_BOTH);
		conflictGroup.setLayoutData(data);
		
		mergeOutput.setMergeResults(null);
		mergeOutput.setMergeResults(mergeOutput.getMergeResults());
		
		Tree tree = new Tree(conflictGroup, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer = new TreeViewer(tree);
		treeViewer.setLabelProvider(new ConflictsLabelProvider());
		treeViewer.setContentProvider(new ConflictsContentProvider());
		treeViewer.setUseHashlookup(true);
		data = new GridData();
		data.heightHint = 200;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;		
		treeViewer.getControl().setLayoutData(data);
		treeViewer.setInput(mergeOutput);
		treeViewer.expandAll();
		
		setMessage(Messages.MergeAbortedWizardPage_message);
		
		setControl(outerContainer);		
	}
	
	public void setMergeOutput(MergeOutput mergeOutput) {
		this.mergeOutput = mergeOutput;
	}
	
	class ConflictsLabelProvider  extends LabelProvider {
		private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		private CompareConfiguration compareConfiguration = new CompareConfiguration();
		private MergeResultsDecorator mergeResultsDecorator = new MergeResultsDecorator();
	
		public Image getImage(Object element) {
			if (element instanceof MergeResult) {
				MergeResult mergeResult = (MergeResult)element;
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
			Image image = workbenchLabelProvider.getImage(mergeResult.getResource());

			if (mergeResult.getAction() != null && mergeResult.getAction().trim().length() > 0) {
				image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.CONFLICTED_CHANGE);
			}
			
			if (mergeResult.getPropertyAction() != null && mergeResult.getPropertyAction().trim().length() > 0) {
				image = mergeResultsDecorator.getImage(image, MergeResultsDecorator.PROPERTY_CONFLICTED_CHANGE);
			}
			
			return image;
		}
		
		public String getText(Object element) {
			if (element instanceof MergeResultsFolder) {
				MergeResultsFolder folder = (MergeResultsFolder)element;
				return folder.toString();
			}
			if (element instanceof MergeResult) {
				MergeResult mergeResult = (MergeResult)element;
				return mergeResult.getResource().getName();
			}
			return super.getText(element);
		}		
	
	}
	
	class ConflictsContentProvider extends WorkbenchContentProvider {
		public Object getParent(Object element) {
			return null;
		}
		
		public boolean hasChildren(Object element) {
			if (element instanceof MergeResultsFolder) return true;
			else return false;
		}
		
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
		
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof MergeOutput) {
				MergeOutput mergeOutput = (MergeOutput)parentElement;
				MergeResult[] rootMergeResults = mergeOutput.getRootMergeResults(true);
				MergeResultsFolder[] mergeResultFolders = mergeOutput.getCompressedFolders(true);
				ArrayList returnObjects = new ArrayList();
				for (int i = 0; i < mergeResultFolders.length; i++) 
					returnObjects.add(mergeResultFolders[i]);				
				for (int i = 0; i < rootMergeResults.length; i++)
					returnObjects.add(rootMergeResults[i]);
				Object[] returnArray = new Object[returnObjects.size()];
				returnObjects.toArray(returnArray);
				return returnArray;
			}
			if (parentElement instanceof MergeResultsFolder) {
				MergeResultsFolder folder = (MergeResultsFolder)parentElement;
				if (folder.isCompressed()) {
					return folder.getMergeResults(true);
				}
			}
			return new Object[0];
		}		
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
