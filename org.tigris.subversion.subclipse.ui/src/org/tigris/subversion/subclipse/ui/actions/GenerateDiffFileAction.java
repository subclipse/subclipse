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
package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.wizards.WizardDialogWithPersistedLocation;
import org.tigris.subversion.subclipse.ui.wizards.generatediff.GenerateDiffFileWizard;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils;

/**
 * Action to generate a patch file using the SVN diff command.
 * 
 */
public class GenerateDiffFileAction extends WorkbenchWindowAction {
	private IResource[] modifiedResources;
	private ArrayList unaddedList;
	private HashMap statusMap;
	
	/** (Non-javadoc)
	 * Method declared on IActionDelegate.
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		statusMap = new HashMap();
		unaddedList = new ArrayList();
		String title = Policy.bind("GenerateSVNDiff.title"); //$NON-NLS-1$
		final IResource[] resources = getSelectedResources();
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				 try {
					modifiedResources = getModifiedResources(resources, monitor);
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}		
			}
			
		}, true, PROGRESS_BUSYCURSOR);
		if (modifiedResources == null || modifiedResources.length == 0) {
			MessageDialog.openInformation(getShell(), Policy.bind("GenerateSVNDiff.title"), Policy.bind("GenerateSVNDiff.noDiffsFoundMsg")); //$NON-NLS-1$ //$NON-NLS-1$
			return;
		}
		IResource[] unaddedResources = new IResource[unaddedList.size()];
		unaddedList.toArray(unaddedResources);
		GenerateDiffFileWizard wizard = new GenerateDiffFileWizard(new StructuredSelection(modifiedResources), unaddedResources, statusMap);
		wizard.setWindowTitle(title);
		wizard.setSelectedResources(getSelectedResources());
		WizardDialog dialog = new WizardDialogWithPersistedLocation(getShell(), wizard, "GenerateDiffFileWizard"); //$NON-NLS-1$
		dialog.setMinimumPageSize(350, 250);
		dialog.open();
	}
	
	protected boolean isEnabled() throws TeamException {
		boolean isEnabled = super.isEnabled();
		return isEnabled;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_DIFF;
	}
	
	protected IResource[] getModifiedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
	    final List modified = new ArrayList();
	    List unversionedFolders = new ArrayList();
	    for (int i = 0; i < resources.length; i++) {
			 IResource resource = resources[i];
			 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			 
			 // This check is for when the action is called with unmanaged resources
			 if (svnResource.getRepository() == null) {
				 continue;
			 }
			 
			 // get adds, deletes, updates and property updates.
			 GetStatusCommand command = new GetStatusCommand(svnResource, true, false);
			 command.run(iProgressMonitor);
			 ISVNStatus[] statuses = command.getStatuses();
			 for (int j = 0; j < statuses.length; j++) {
			     if (SVNStatusUtils.isReadyForCommit(statuses[j]) || SVNStatusUtils.isMissing(statuses[j])) {
			         IResource currentResource = SVNWorkspaceRoot.getResourceFor(resource, statuses[j]);
			         if (currentResource != null) {
			             ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(currentResource);
			             if (!localResource.isIgnored()) {
			                 if (!SVNStatusUtils.isManaged(statuses[j])) {
			                	if (!isSymLink(currentResource)) {
				                 	if (currentResource.getType() != IResource.FILE)
				                 		unversionedFolders.add(currentResource);
				                 	else
				                 		if (!modified.contains(currentResource)) {
				                 			modified.add(currentResource);
				                 			if (currentResource instanceof IContainer) statusMap.put(currentResource, statuses[j].getPropStatus());
				                 			else statusMap.put(currentResource, statuses[j].getTextStatus());
				                 			if (addToUnadded(currentResource)) unaddedList.add(currentResource);
				                 		}
			                	}
			                 } else
			                	 if (!modified.contains(currentResource)) {
			                		 modified.add(currentResource);
			                 		 if (currentResource instanceof IContainer) statusMap.put(currentResource, statuses[j].getPropStatus());
			                 		 else statusMap.put(currentResource, statuses[j].getTextStatus());
			                	 }
			             }
			         }
			     }
			 }
	    }
	    // get unadded resources and add them to the list.
	    IResource[] unaddedResources = getUnaddedResources(unversionedFolders, iProgressMonitor);
	    for (int i = 0; i < unaddedResources.length; i++) {
	    	if (!modified.contains(unaddedResources[i])) {
	    		if (unaddedResources[i].getType() == IResource.FILE) {
	    			modified.add(unaddedResources[i]);
	    			statusMap.put(unaddedResources[i], SVNStatusKind.UNVERSIONED);
	    		}
	    		if (addToUnadded(unaddedResources[i])) unaddedList.add(unaddedResources[i]);
	    	}
	    }
	    return (IResource[]) modified.toArray(new IResource[modified.size()]);
	}	
	
	private IResource[] getUnaddedResources(List resources, IProgressMonitor iProgressMonitor) throws SVNException {
		final List unadded = new ArrayList();
		final SVNException[] exception = new SVNException[] { null };
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
	        if (resource.exists()) {
			    // visit each resource deeply
			    try {
				    resource.accept(new IResourceVisitor() {
					public boolean visit(IResource aResource) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(aResource);
						// skip ignored resources and their children
						try {
							if (svnResource.isIgnored())
								return false;
							// visit the children of shared resources
							if (svnResource.isManaged())
								return true;
							if ((aResource.getType() == IResource.FOLDER) && isSymLink(aResource)) // don't traverse into symlink folders
								return false;
						} catch (SVNException e) {
							exception[0] = e;
						}
						// file/folder is unshared so record it
						unadded.add(aResource);
						return aResource.getType() == IResource.FOLDER;
					}
				}, IResource.DEPTH_INFINITE, false /* include phantoms */);
			    } catch (CoreException e) {
				    throw SVNException.wrapException(e);
			    }
			    if (exception[0] != null) throw exception[0];
	        }
		}
		return (IResource[]) unadded.toArray(new IResource[unadded.size()]);
	}
		
	
	protected boolean isSymLink(IResource resource) {
		File file = resource.getLocation().toFile();
	    try {
	    	if (!file.exists())
	    		return true;
	    	else {
	    		String cnnpath = file.getCanonicalPath();
	    		String abspath = file.getAbsolutePath();
	    		return !abspath.equals(cnnpath);
	    	}
	    } catch(IOException ex) {
	      return true;
	    }	
	}
	
	private boolean addToUnadded(IResource resource) {
		IResource parent = resource;
		while (parent != null) {
			parent = parent.getParent();
			if (unaddedList.contains(parent)) return false;
		}
		return true;
	}

}
