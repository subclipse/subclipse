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
package com.collabnet.subversion.merge;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

import com.collabnet.subversion.merge.wizards.MergeWizardAdvancedPage;
import com.collabnet.subversion.merge.wizards.MergeWizardLastPage;
import com.collabnet.subversion.merge.wizards.MergeWizardMainPage;

public class AdvancedMergeInputProvider implements IMergeInputProvider {
	private String text;
	private String description;
	private Image image;
	private int sequence;
	private MergeWizardAdvancedPage advancedPage;
	private WizardPage[] wizardPages;

	public String getText() {
		return text;
	}
	
	public int getSequence() {
		return sequence;
	}

	public IWizardPage[] getWizardPages(boolean initializePages) {
		if (wizardPages == null || initializePages) {
			advancedPage = new MergeWizardAdvancedPage("advanced", Messages.AdvancedMergeInputProvider_selectMergeSource, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD)); //$NON-NLS-1$
			WizardPage[] pages = { advancedPage };
			wizardPages = pages;
		}
		return wizardPages;
	}
	
	public IWizardPage getNextPage(IWizardPage currentPage) {
		return null;
	}	

	public boolean performMerge(MergeWizardMainPage mainPage, MergeWizardLastPage optionsPage, IWorkbenchPart targetPart) {
		String commonRoot = advancedPage.getCommonRoot(false);
		String mergeFrom = advancedPage.getMergeFrom();
		Activator.getDefault().saveMergeSource(mergeFrom, commonRoot);
		Activator.getDefault().getDialogSettings().put("mergeToUrl_" + mergeFrom, advancedPage.getMergeTarget());
		
		IResource[] resources = advancedPage.getResources();		
		
		MergeOperation mergeOperation = new MergeOperation(targetPart, resources, advancedPage.getFromUrls(), advancedPage.getFromRevision(), advancedPage.getToUrls(), advancedPage.getToRevision(), null, null);
		mergeOperation.setForce(optionsPage.isForce());
		mergeOperation.setIgnoreAncestry(optionsPage.isIgnore());
		mergeOperation.setDepth(optionsPage.getDepth());
		mergeOperation.setTextConflictHandling(optionsPage.getTextConflictHandling());
		mergeOperation.setBinaryConflictHandling(optionsPage.getBinaryConflictHandling());
		mergeOperation.setPropertyConflictHandling(optionsPage.getPropertyConflictHandling());
		mergeOperation.setTreeConflictHandling(optionsPage.getTreeConflictHandling());
		try {
			mergeOperation.run();
		} catch (Exception e) {
			Activator.handleError(Messages.AdvancedMergeInputProvider_error, e);
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.AdvancedMergeInputProvider_merge, e.getMessage());
			return false;
		}
		return true;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int compareTo(Object compareToObject) {
		if (!(compareToObject instanceof IMergeInputProvider)) return 0;
		IMergeInputProvider compareToInputProvider = (IMergeInputProvider)compareToObject;
		if (getSequence() > compareToInputProvider.getSequence()) return 1;
		else if (compareToInputProvider.getSequence() > getSequence()) return -1;
		return getText().compareTo(compareToInputProvider.getText());
	}

	public boolean enabledForMultipleSelection() {
		return false;
	}

	public boolean showOptionsPage() {
		return true;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean hideDepth() {
		return false;
	}

	public boolean hideForce() {
		return false;
	}

	public boolean hideIgnoreAncestry() {
		return false;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public boolean showBestPracticesPage() {
		return true;
	}

}
