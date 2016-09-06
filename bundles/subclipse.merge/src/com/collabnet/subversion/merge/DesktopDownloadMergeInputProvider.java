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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

import com.collabnet.subversion.merge.wizards.MergeWizardDesktopDownloadPage;
import com.collabnet.subversion.merge.wizards.MergeWizardLastPage;
import com.collabnet.subversion.merge.wizards.MergeWizardMainPage;

public class DesktopDownloadMergeInputProvider implements IChangeSetMergeInputProvider {
	private MergeWizardDesktopDownloadPage downloadPage;
	private WizardPage[] wizardPages;
	
	public boolean enabledForMultipleSelection() {
		return true;
	}

	public String getDescription() {
		return "Use this method to merge the revisions associated with one or more CollabNet artifacts.  Typically this option would be used to backport fixes to a release branch or similar scenarios where all of the changes associated with an artifact need to be merged from one location to another.";
	}

	public Image getImage() {
		return Activator.getImage(Activator.IMAGE_CHANGE_SETS);
	}

	public IWizardPage getNextPage(IWizardPage currentPage) {
		return null;
	}

	public int getSequence() {
		return 20;
	}

	public String getText() {
		return "Change-set based merge";
	}

	public IWizardPage[] getWizardPages(boolean initializePages) {
		if (wizardPages == null || initializePages) {
			downloadPage = new MergeWizardDesktopDownloadPage("download", "Change-set merge", Activator.getDefault().getImageDescriptor(Activator.IMAGE_COLLABNET_WIZBAN)); //$NON-NLS-1$
			WizardPage[] pages = { downloadPage };
			wizardPages = pages;
		}
		return wizardPages;
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

	public boolean performMerge(MergeWizardMainPage mainPage, MergeWizardLastPage optionsPage, IWorkbenchPart targetPart) {
		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Download Desktop", "This is where you will download this stuff.");
		return false;
	}

	public void setDescription(String description) {
	}

	public void setImage(Image image) {
	}

	public void setSequence(int sequence) {
	}

	public void setText(String text) {
	}

	public boolean showBestPracticesPage() {
		return false;
	}

	public boolean showOptionsPage() {
		return false;
	}

	public int compareTo(Object compareToObject) {
		if (!(compareToObject instanceof IMergeInputProvider)) return 0;
		IMergeInputProvider compareToInputProvider = (IMergeInputProvider)compareToObject;
		if (getSequence() > compareToInputProvider.getSequence()) return 1;
		else if (compareToInputProvider.getSequence() > getSequence()) return -1;
		return getText().compareTo(compareToInputProvider.getText());
	}

}
