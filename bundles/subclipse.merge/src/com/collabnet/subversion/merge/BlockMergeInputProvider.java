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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.collabnet.subversion.merge.wizards.MergeWizardLastPage;
import com.collabnet.subversion.merge.wizards.MergeWizardMainPage;
import com.collabnet.subversion.merge.wizards.MergeWizardRevisionsPage;
import com.collabnet.subversion.merge.wizards.MergeWizardStandardPage;

public class BlockMergeInputProvider implements IMergeInputProvider {
	private String text;
	private String description;
	private Image image;
	private int sequence;
	private MergeWizardStandardPage standardPage;
	private MergeWizardRevisionsPage revisionsPage;
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
			standardPage = new MergeWizardStandardPage("standardBlock", Messages.BlockMergeInputProvider_selectMergeSource, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD), Messages.BlockMergeInputProvider_specifyLocations, false, false); //$NON-NLS-1$
			revisionsPage = new MergeWizardRevisionsPage("revisionsBlock", Messages.BlockMergeInputProvider_selectRevisions, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD), standardPage, Messages.BlockMergeInputProvider_specifyRevisions); //$NON-NLS-1$
			WizardPage[] pages = { standardPage, revisionsPage };
			wizardPages = pages;
		}
		return wizardPages;
	}

	public boolean performMerge(MergeWizardMainPage mainPage, MergeWizardLastPage optionsPage, IWorkbenchPart targetPart) {		
		IResource[] resources = standardPage.getResources();
		SVNUrl[] urls = standardPage.getUrls();
		
		SVNRevisionRange[] revisions = revisionsPage.getRevisions();	

		Set<IResource> usedResources = new HashSet<IResource>();
		Map<SVNRevision.Number, List<IResource>> map = revisionsPage
				.getRevisionToResource();
		if (map.size() > 0) {
			ILogEntry[] entries = revisionsPage.getSelectedLogEntries();
			for (int i = 0; i < entries.length; i++) {
				List<IResource> lst = map.get(entries[i].getRevision());
				usedResources.addAll(lst);
			}
			// only filter the urls if the usedResources does have content.
			if (usedResources.size() > 0) {
				List<SVNUrl> urlsList = new ArrayList<SVNUrl>();
				List<IResource> resourcesList = new ArrayList<IResource>();
				
				for (int i = 0; i < resources.length; i++) {
					if (usedResources.contains(resources[i])) {
						urlsList.add(urls[i]);
						resourcesList.add(resources[i]);
					}
				}
				resources = resourcesList
				.toArray(new IResource[resourcesList.size()]);
				urls = urlsList.toArray(new SVNUrl[urlsList.size()]);
			}
		}
		
		MergeOperation mergeOperation = new MergeOperation(targetPart, resources, urls, null, urls, null, revisions, null);		
		mergeOperation.setRecordOnly(true);
		try {
			mergeOperation.run();
		} catch (Exception e) {
			Activator.handleError(Messages.BlockMergeInputProvider_error, e);
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.BlockMergeInputProvider_merge, e.getMessage());
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
