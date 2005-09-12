/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.PromptingDialog;
import org.tigris.subversion.subclipse.ui.util.IPromptCondition;

/**
 * This class represents an action performed on a local SVN workspace
 */
public abstract class WorkspaceAction extends SVNAction {


	/**
	 * Most SVN workspace actions modify the workspace and thus should
	 * save dirty editors.
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#needsToSaveDirtyEditors()
	 */
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	/**
	 * The action is enabled for the appropriate resources. This method checks
	 * that:
	 * <ol>
	 * <li>there is no overlap between a selected file and folder (overlapping
	 * folders is allowed because of logical vs. physical mapping problem in
	 * views)
	 * <li>the state of the resources match the conditions provided by:
	 * <ul>
	 * <li>isEnabledForIgnoredResources()
	 * <li>isEnabledForManagedResources()
	 * <li>isEnabledForUnManagedResources() (i.e. not ignored and not managed)
	 * </ul>
	 * </ol>
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		
		// invoke the inherited method so that overlaps are maintained
		IResource[] resources = super.getSelectedResources();
		
		// disable if no resources are selected
		if(resources.length==0) return false;
		
		// disable properly for single resource enablement
		if (!isEnabledForMultipleResources() && resources.length != 1) return false;
		
		// validate enabled for each resource in the selection
		List folderPaths = new ArrayList();
		List filePaths = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			
			// only enable for accessible resources
			if ((! resource.isAccessible()) && (!isEnabledForInaccessibleResources()))
                return false;
			
			// no SVN actions are enabled if the selection contains a linked resource
			if (SVNWorkspaceRoot.isLinkedResource(resource)) return false;
			
			// only enable for resources in a project shared with SVN
			if(RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId()) == null) {
				return false;
			}
			
			// collect files and folders separately to check for overlap later	
			IPath resourceFullPath = resource.getFullPath();
			if(resource.getType() == IResource.FILE) {
				filePaths.add(resourceFullPath);
			} else {
				folderPaths.add(resourceFullPath);
			}
			
			// ensure that resource management state matches what the action requires
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (!isEnabledForSVNResource(svnResource)) {
				return false;
			}
		}
		// Ensure that there is no overlap between files and folders
		// NOTE: folder overlap must be allowed because of logical vs. physical
		if(!folderPaths.isEmpty()) {
			for (Iterator fileIter = filePaths.iterator(); fileIter.hasNext();) {
				IPath resourcePath = (IPath) fileIter.next();
				for (Iterator it = folderPaths.iterator(); it.hasNext();) {
					IPath folderPath = (IPath) it.next();
					if (folderPath.isPrefixOf(resourcePath)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
    /**
     * Normally, actions are not availables for inaccessible resources 
     * (files or folder which do not exist ...) 
     */
    protected boolean isEnabledForInaccessibleResources() {
        return false;
    }
    
	/**
	 * Method isEnabledForSVNResource.
	 * @param svnResource
	 * @return boolean
	 */
	protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) throws SVNException {
		boolean managed = false;
		boolean ignored = false;
		boolean added = false;
		if (svnResource.isIgnored()) {
			ignored = true;
		} else {
            managed = svnResource.isManaged();
			if (managed) {
                added = svnResource.getStatus().isAdded();
            }
		}
		if (managed && ! isEnabledForManagedResources()) return false;
		if ( ! managed && ! isEnabledForUnmanagedResources()) return false;
		if ( ignored && ! isEnabledForIgnoredResources()) return false;
		if (added && ! isEnabledForAddedResources()) return false;
		return true;
	}
	
	/**
	 * Method isEnabledForIgnoredResources.
	 * @return boolean
	 */
	protected boolean isEnabledForIgnoredResources() {
		return false;
	}
	
	/**
	 * Method isEnabledForUnmanagedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	/**
	 * Method isEnabledForManagedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForManagedResources() {
		return true;
	}

	/**
	 * Method isEnabledForAddedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForAddedResources() {
		return true;
	}
	
	/**
	 * Method isEnabledForAddedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForMultipleResources() {
		return true;
	}
	
	/**
	 * Method getNonOverlapping ensures that a resource is not covered more than once.
	 * @param resources
	 * @return IResource[]
	 */
	public static IResource[] getNonOverlapping(IResource[] resources) {
		// Sort the resources so the shortest paths are first
		List sorted = new ArrayList();
		sorted.addAll(Arrays.asList(resources));
		Collections.sort(sorted, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				IResource resource0 = (IResource) arg0;
				IResource resource1 = (IResource) arg1;
				return resource0.getFullPath().segmentCount() - resource1.getFullPath().segmentCount();
			}
			public boolean equals(Object arg0) {
				return false;
			}
		});
		// Collect all non-overlapping resources
		List coveredPaths = new ArrayList();
		for (Iterator iter = sorted.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			IPath resourceFullPath = resource.getFullPath();
			boolean covered = false;
			for (Iterator it = coveredPaths.iterator(); it.hasNext();) {
				IPath path = (IPath) it.next();
				if(path.isPrefixOf(resourceFullPath)) {
					covered = true;
				}
			}
			if (covered) {
				// if the resource is covered by a parent, remove it
				iter.remove();
			} else {
				// if the resource is a non-covered folder, add it to the covered paths
				if (resource.getType() == IResource.FOLDER) {
					coveredPaths.add(resource.getFullPath());
				}
			}
		}
		return (IResource[]) sorted.toArray(new IResource[sorted.size()]);
	}
	
	/**
	 * Override to ensure that the selected resources so not overlap.
	 * This method assumes that all actions are deep.
	 * 
	 * @see org.tigris.subversion.subclipse.ui.actions.TeamAction#getSelectedResources()
	 */
	protected IResource[] getSelectedResources() {
		return getNonOverlapping(super.getSelectedResources());
	}

	/**
	 * Prompts user to overwrite resources that are in the <code>resources<code> list and are modified
	 * @param resources Resources to prompt for overwrite if modified
	 * @return Array of resources that the user did want overwriting
	 * @throws SVNException Exception getting state of SVN resources
	 * @throws InterruptedException Prompt dialog was shut down abnormally
	 */
	protected IResource[] checkOverwriteOfDirtyResources(IResource[] resources) throws SVNException, InterruptedException {
		List dirtyResources = new ArrayList();
		
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (svnResource.isDirty()) {
				dirtyResources.add(resource);
			}			
		}
		
		PromptingDialog dialog = new PromptingDialog(getShell(), resources, 
				getPromptCondition((IResource[]) dirtyResources.toArray(new IResource[dirtyResources.size()])), Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
		return dialog.promptForMultiple();
	}

	/**
	 * This is a helper for the SVN UI automated tests. It allows the tests to ignore prompting dialogs.
	 * @param resources
	 */
	protected IPromptCondition getPromptCondition(IResource[] resources) {
		return getOverwriteLocalChangesPrompt(resources);
	}
}
