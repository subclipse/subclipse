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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.IChangeSetMergeInput;
import com.collabnet.subversion.merge.IChangeSetMergeInputProvider;
import com.collabnet.subversion.merge.IMergeInputProvider;
import com.collabnet.subversion.merge.Messages;

public class MergeWizard extends Wizard {
	private IResource[] resources;
	private IChangeSetMergeInput[] changeSetMergeInputs;
	private IWorkbenchPart targetPart;
	private IMergeInputProvider[] mergeInputProviders;
	private MergeWizardMainPage mainPage;
	private MergeWizardBestPracticesPage bestPracticesPage;

	private MergeWizardLastPage lastPage;
//	private MergeWizardResourceSelectionPage resourceSelectionPage;
	private IWizardPage[][] pages;
//	private ChangeSetMergeInputProvider changeSetMergeInputProvider;
	private IChangeSetMergeInputProvider changeSetMergeInputProvider;
	
	private String commonRoot;

	private String[] urlStrings;
	
	private boolean checkBestPractices;
	private boolean retrieveRevisionsMethodChanged;
	
	public final static String LAST_RELATIVE_PATH_CHOICE = "MergeWizard.relativePath"; //$NON-NLS-1$
	public final static String LAST_RETRIEVE_ELIGIBLE_REVISIONS_SEPARATELY = "MergeWizard.retrieveEligibleRevisionsSeparately"; //$NON-NLS-1$
	
	public MergeWizard(IResource[] resources, IWorkbenchPart targetPart) {
		super();
		this.resources = resources;
		this.targetPart = targetPart;
	}
	
	public MergeWizard(IChangeSetMergeInput[] changeSetMergeInputs, IWorkbenchPart targetPart) {
		super();
		this.changeSetMergeInputs = changeSetMergeInputs;
		this.targetPart = targetPart;
	}
	
	public void addPages() {
		super.addPages();
		try {
			mergeInputProviders = Activator.getMergeInputProviders();
		} catch (Exception e) {
			Activator.handleError(e);
		}
		setWindowTitle(Messages.MergeWizard_title);
		
		if (changeSetMergeInputs == null) {
			mainPage = new MergeWizardMainPage("main", Messages.MergeWizard_selectType, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD), mergeInputProviders); //$NON-NLS-1$
			addPage(mainPage);
		}
//		else {
//			resourceSelectionPage = new MergeWizardResourceSelectionPage("resourceSelection", "Select the merge target", Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD));
//			addPage(resourceSelectionPage);
//		}
		
		bestPracticesPage = new MergeWizardBestPracticesPage("bestPractices", Messages.MergeWizard_bestPractices, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD)); //$NON-NLS-1$
		if (changeSetMergeInputs == null) addPage(bestPracticesPage);
		pages = new WizardPage[mergeInputProviders.length][];
		for (int i = 0; i < mergeInputProviders.length; i++) {
			IWizardPage[] providerPages = mergeInputProviders[i].getWizardPages(true);
			pages[i] = providerPages;		
			if (changeSetMergeInputs != null && mergeInputProviders[i] instanceof IChangeSetMergeInputProvider)
				changeSetMergeInputProvider = (IChangeSetMergeInputProvider)mergeInputProviders[i];
		}
		for (int i = 0; i < pages.length; i++) {
			IWizardPage[] providerPages = pages[i];
			if (providerPages != null) {
				if (changeSetMergeInputs == null || mergeInputProviders[i] instanceof IChangeSetMergeInputProvider) {
					for (int j = 0; j < providerPages.length; j++) {
						addPage(providerPages[j]);
						if (changeSetMergeInputs != null && providerPages[j] instanceof IMergeWizardChangeSetPage)
							addPage(bestPracticesPage);
					}
				}
			}
		}
		lastPage = new MergeWizardLastPage("last", Messages.MergeWizard_selectOptions, Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD)); //$NON-NLS-1$
		addPage(lastPage);		
	}
	
	public boolean canFinish() {
		if ((mainPage != null && !mainPage.isPageComplete()) || !lastPage.isPageComplete()) return false;
		if ((mainPage != null && mainPage.showBestPracticesPage()) && !bestPracticesPage.isPageComplete()) return false;		
		IWizardPage[] providerPages;
		if (changeSetMergeInputProvider == null) providerPages = mainPage.getSelectedMergeInputProvider().getWizardPages(false);
		else providerPages = changeSetMergeInputProvider.getWizardPages(false);
		if (providerPages == null) return true;
		for (int i = 0; i < providerPages.length; i++) {
			if (!providerPages[i].isPageComplete()) return false;
		}
		return true;
	}
	
	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof MergeWizardLastPage) return null;
		if (page instanceof MergeWizardMainPage) {
			if (mainPage.showBestPracticesPage() && checkBestPractices) {
				if (bestPracticesPage.isPageShown()) return bestPracticesPage;
				else {
					if (bestPracticesPage.needsChecks()) bestPracticesPage.performChecks(true);
					if (bestPracticesPage.hasWarnings()) return bestPracticesPage;
				}
			}
			
			checkBestPractices = true;
			
			IWizardPage[] providerPages = mainPage.getSelectedMergeInputProvider().getWizardPages(false);
			if (providerPages == null || providerPages.length == 0) {
				if (!Activator.getDefault().isDesktopInstalled()) {
					return null;
				} else {
					return lastPage;
				}
			}
			return providerPages[0];
		}
		if (page instanceof IMergeWizardChangeSetPage) {
			if (changeSetMergeInputs != null) {
				IMergeWizardChangeSetPage changeSetPage = (IMergeWizardChangeSetPage)page;
				if (changeSetPage.showBestPracticesPage()) {
					if (bestPracticesPage.isPageShown()) return bestPracticesPage;
					else {
						if (bestPracticesPage.needsChecks()) bestPracticesPage.performChecks(true);
						if (bestPracticesPage.hasWarnings()) return bestPracticesPage;
					}					
				}
			}
		}
//		if (page instanceof MergeWizardResourceSelectionPage) {
//			if (resourceSelectionPage.showBestPracticesPage()) {
//				if (bestPracticesPage.isPageShown()) return bestPracticesPage;
//				else {
//					bestPracticesPage.performChecks(true);
//					if (bestPracticesPage.hasWarnings()) return bestPracticesPage;
//				}
//			}			
//			
//			IWizardPage[] providerPages = changeSetMergeInputProvider.getWizardPages(false);
//			return providerPages[0];
//		}
		if (page instanceof MergeWizardBestPracticesPage) {
			IWizardPage[] providerPages;
			if (changeSetMergeInputProvider == null)
				providerPages = mainPage.getSelectedMergeInputProvider().getWizardPages(false);
			else
				providerPages = changeSetMergeInputProvider.getWizardPages(false);
			if (providerPages == null || providerPages.length == 0) return lastPage;
			if (changeSetMergeInputProvider == null)
				return providerPages[0];	
			else
				return providerPages[1];
		}
		IWizardPage nextPage;
		if (changeSetMergeInputProvider == null)
			nextPage = mainPage.getSelectedMergeInputProvider().getNextPage(page);
		else
			nextPage = changeSetMergeInputProvider.getNextPage(page);
		if (nextPage == null) {
			if (mainPage != null && mainPage.getSelectedMergeInputProvider().showOptionsPage()) return lastPage;
			if (changeSetMergeInputProvider != null && changeSetMergeInputProvider.showOptionsPage()) return lastPage;
		}
		return nextPage;
	}	
	
	public boolean performFinish() {
		IMergeInputProvider selectedMergeInputProvider;
		if (changeSetMergeInputProvider == null)
			selectedMergeInputProvider = mainPage.getSelectedMergeInputProvider();
		else
			selectedMergeInputProvider = changeSetMergeInputProvider;
		return selectedMergeInputProvider.performMerge(mainPage, lastPage, targetPart);
	}

	public IResource[] getResources() {
		return resources;
	}
	
	public IChangeSetMergeInput[] getChangeSetMergeInputs() {
		return changeSetMergeInputs;
	}

	// This will go.
	public IResource getResource() {
		resources = getResources();
		if (resources == null) return null;
		return resources[0];
	}

	public void setResources(IResource[] resources) {
		this.resources = resources;
	}
	
	public MergeWizardBestPracticesPage getBestPracticesPage() {
		return bestPracticesPage;
	}
	
	public boolean isRetrieveRevisionsMethodChanged() {
		return retrieveRevisionsMethodChanged;
	}

	public void setRetrieveRevisionsMethodChanged(
			boolean retrieveRevisionsMethodChanged) {
		this.retrieveRevisionsMethodChanged = retrieveRevisionsMethodChanged;
	}
	
	public boolean suggestMergeSources() {
		return SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SUGGEST_MERGE_SOURCES);
	}

	public boolean needsProgressMonitor() {
		return true;
	}
	
	public IMergeInputProvider getSelectedMergeInputProvider() {
		if (changeSetMergeInputProvider == null)
			return mainPage.getSelectedMergeInputProvider();
		else
			return changeSetMergeInputProvider;
	}
	
	public void setCommonRoot(String commonRoot) {
		this.commonRoot = commonRoot;
	}
	
	@SuppressWarnings("unchecked")
	public String getCommonRoot() {
		if (commonRoot == null) {
	    	ArrayList urlList = new ArrayList();
	    	for (int i = 0; i < resources.length; i++) {
	    		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
	    		try {
	                String anUrl = svnResource.getStatus().getUrlString();
	                if (anUrl != null) urlList.add(anUrl);
	            } catch (SVNException e1) {}    		
	    	}
	    	urlStrings = new String[urlList.size()];
	    	urlList.toArray(urlStrings);
	    	if (urlStrings.length == 0) return null;
	    	String urlString = urlStrings[0];
	    	if (urlStrings.length == 1) return urlString;
	    	commonRoot = null;
	    	tag1:
	    	for (int i = 0; i < urlString.length(); i++) {
	    		String partialPath = urlString.substring(0, i+1);
	    		if (partialPath.endsWith("/")) { //$NON-NLS-1$
		    		for (int j = 1; j < urlStrings.length; j++) {
		    			if (!urlStrings[j].startsWith(partialPath)) break tag1;
		    		}
		    		commonRoot = partialPath.substring(0, i);
	    		}
	    	}			
		}
		return commonRoot;
	}
	
	public String[] getUrlStrings() {
		if (commonRoot == null) commonRoot = getCommonRoot();
		return urlStrings;
	}

	public IWorkbenchPart getTargetPart() {
		return targetPart;
	}

}
