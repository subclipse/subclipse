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
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;

import com.collabnet.subversion.merge.wizards.MergeWizardLastPage;
import com.collabnet.subversion.merge.wizards.MergeWizardMainPage;
import com.collabnet.subversion.merge.wizards.MergeWizardStandardPage;
import com.collabnet.subversion.merge.wizards.MergeWizardUnblockRevisionsPage;

public class UnblockMergeInputProvider implements IMergeInputProvider {
	private String text;
	private String description;
	private int sequence;
	private Image image;
	private MergeWizardStandardPage standardPage;
	private MergeWizardUnblockRevisionsPage revisionsPage;
	private WizardPage[] wizardPages;

	public boolean enabledForMultipleSelection() {
		return true;
	}

	public IWizardPage getNextPage(IWizardPage currentPage) {
		if (currentPage == standardPage) return revisionsPage;
		else return null;
	}

	public int getSequence() {
		return sequence;
	}

	public String getText() {
		return text;
	}

	public IWizardPage[] getWizardPages(boolean initializePages) {
		if (wizardPages == null || initializePages) {
			standardPage = new MergeWizardStandardPage("standardUnblock", Messages.UnblockMergeInputProvider_selectMergeSource, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD), Messages.UnblockMergeInputProvider_specifyLocation, false, true); //$NON-NLS-1$
			revisionsPage = new MergeWizardUnblockRevisionsPage("revisionsUnblock", Messages.UnblockMergeInputProvider_selectRevisions, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD), standardPage); //$NON-NLS-1$
			WizardPage[] pages = { standardPage, revisionsPage };
			wizardPages = pages;
		}
		return wizardPages;
	}

	public boolean performMerge(MergeWizardMainPage mainPage, MergeWizardLastPage optionsPage, IWorkbenchPart targetPart) {		
		IResource[] resources = standardPage.getResources();
		
		SVNRevisionRange[] revisions = null;	
		revisions = revisionsPage.getRevisions();

		MergeOperation mergeOperation = new MergeOperation(targetPart, resources, standardPage.getUrls(), null, standardPage.getUrls(), null, revisions, null);		
		mergeOperation.setRecordOnly(true);
		mergeOperation.setUnblock(true);
		try {
			mergeOperation.run();
		} catch (Exception e) {
			Activator.handleError(Messages.UnblockMergeInputProvider_error, e);
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UnblockMergeInputProvider_merge, e.getMessage());
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

	public boolean showOptionsPage() {
		return false;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean hideDepth() {
		return true;
	}

	public boolean hideForce() {
		return true;
	}

	public boolean hideIgnoreAncestry() {
		return true;
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
		return false;
	}

}
