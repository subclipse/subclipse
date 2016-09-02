/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.generatediff;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * A wizard for creating a patch file by running the SVN diff command.
 */
public class GenerateDiffFileWizard extends Wizard {
	
	private PatchFileSelectionPage mainPage;
	
	private IStructuredSelection selection;
	private IResource[] unaddedResources;
	private HashMap statusMap;
	private IResource[] selectedResources;

	private OptionsPage optionsPage;

	// end of PatchFileCreationOptionsPage
	
	public GenerateDiffFileWizard(IStructuredSelection selection, IResource[] unaddedResources, HashMap statusMap) {
		super();
		this.selection = selection;
		this.unaddedResources = unaddedResources;
		this.statusMap = statusMap;
		setWindowTitle(Policy.bind("GenerateSVNDiff.title")); //$NON-NLS-1$
		initializeDefaultPageImageDescriptor();
	}

	public void addPages() {
		String pageTitle = Policy.bind("GenerateSVNDiff.pageTitle"); //$NON-NLS-1$
		String pageDescription = Policy.bind("GenerateSVNDiff.pageDescription"); //$NON-NLS-1$
		mainPage = new PatchFileSelectionPage(pageTitle, pageTitle, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_DIFF), selection, statusMap);
		mainPage.setDescription(pageDescription);
		
		pageTitle = Policy.bind("GenerateSVNDiff.AdvancedOptions"); //$NON-NLS-1$
		pageDescription = Policy.bind("GenerateSVNDiff.ConfigureOptions"); //$NON-NLS-1$
		optionsPage = new OptionsPage(pageTitle, pageTitle, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_DIFF));
		optionsPage.setDescription(pageDescription);
		addPage(mainPage);
		addPage(optionsPage);
		
	}
		
	/**
	 * Initializes this creation wizard using the passed workbench and
	 * object selection.
	 *
	 * @param workbench the current workbench
	 * @param selection the current object selection
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
	/**
	 * Declares the wizard banner iamge descriptor
	 */
	protected void initializeDefaultPageImageDescriptor() {
		String iconPath;
		iconPath = "icons/full/"; //$NON-NLS-1$
		try {
			URL installURL = SVNUIPlugin.getPlugin().getBundle().getEntry("/"); //$NON-NLS-1$
			URL url = new URL(installURL, iconPath + "wizards/newconnect_wiz.gif");	//$NON-NLS-1$
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			setDefaultPageImageDescriptor(desc);
		} catch (MalformedURLException e) {
			// Should not happen.  Ignore.
		}
	}
	
	/* (Non-javadoc)
	 * Method declared on IWizard.
	 */
	public boolean needsProgressMonitor() {
		return true;
	}
	/**
	 * Completes processing of the wizard. If this method returns <code>
	 * true</code>, the wizard will close; otherwise, it will stay active.
	 */
	public boolean performFinish() {
		String fs = mainPage.getFilesystemFile();
		IFile ws = mainPage.getWorkspaceFile();
		int type = mainPage.getSaveType();
		boolean eclipseFormat = optionsPage.isMultiPatch();
		boolean projectRelative = optionsPage.isProjectRelative();
		
		try {
			if(type != mainPage.CLIPBOARD) {
				File file = new File(fs!=null ? fs : ws.getLocation().toOSString());
				if (file.exists()) {
					// prompt then delete
					String title = Policy.bind("GenerateSVNDiff.overwriteTitle"); //$NON-NLS-1$
					String msg = Policy.bind("GenerateSVNDiff.overwriteMsg"); //$NON-NLS-1$
					final MessageDialog dialog = new MessageDialog(getShell(), title, null, msg, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL }, 0);

					dialog.open();
	
					if (dialog.getReturnCode() != 0) {
						// cancel
						return false;
					}
				}
				GenerateDiffFileOperation generateDiffFileOperation = new GenerateDiffFileOperation(getResources(), getUnaddedResources(), file, false, false, eclipseFormat, projectRelative, getShell());
				generateDiffFileOperation.setSelectedResources(selectedResources);
				getContainer().run(true, true, generateDiffFileOperation);
				if(type==mainPage.WORKSPACE) {
					ws.getParent().refreshLocal(IResource.DEPTH_ONE, null);
				}
			} else {
				GenerateDiffFileOperation generateDiffFileOperation = new GenerateDiffFileOperation(getResources(), getUnaddedResources(), null, true, false, eclipseFormat, projectRelative, getShell());
				generateDiffFileOperation.setSelectedResources(selectedResources);
				getContainer().run(true, true, generateDiffFileOperation);
			}
			return true;
		} catch (InterruptedException e1) {
			return true;
		} catch(CoreException e) {
			SVNUIPlugin.openError(getShell(), Policy.bind("GenerateSVNDiff.error"), null, e); //$NON-NLS-1$
			return false;
		} catch (InvocationTargetException e2) {
			SVNUIPlugin.openError(getShell(), Policy.bind("GenerateSVNDiff.error"), null, e2); //$NON-NLS-1$
			return false;
		}
	}
	
	protected IResource[] getResources() {
		return mainPage.getSelectedResources();
	}
	
	private IResource[] getUnaddedResources() {
		ArrayList unaddedResourceList = new ArrayList();
		for (int i = 0; i < unaddedResources.length; i++)
			unaddedResourceList.add(unaddedResources[i]);
		ArrayList selectedUnaddedResourceList = new ArrayList();
		IResource[] selectedResources = getResources();
		for (int i = 0; i < selectedResources.length; i++) {
			if (unaddedResourceList.contains(selectedResources[i])) {
				selectedUnaddedResourceList.add(selectedResources[i]);
			} else {
				IResource unaddedParent = getUnaddedParent(selectedResources[i], unaddedResourceList);
				if (unaddedParent != null && !selectedUnaddedResourceList.contains(unaddedParent))
					selectedUnaddedResourceList.add(unaddedParent);
			}
		}
		IResource[] unaddedResourceArray = new IResource[selectedUnaddedResourceList.size()];
		selectedUnaddedResourceList.toArray(unaddedResourceArray);
		return unaddedResourceArray;
	}
	
	private IResource getUnaddedParent(IResource resource, ArrayList unaddedResourceList) {
		IResource parent = resource;
		while (parent != null) {
			parent = parent.getParent();
			int index = unaddedResourceList.indexOf(parent);
			if (index != -1) return (IResource)unaddedResourceList.get(index);
		}
		return null;
	}

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}
}
