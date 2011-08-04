/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
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
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public class CopyAction extends WorkbenchWindowAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final IResource[] resources = getSelectedResources();
		final IProject project = resources[0].getProject();
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), project, false, Policy.bind("CopyAction.selectionLabel")); //$NON-NLS-1$
		if (dialog.open() == ContainerSelectionDialog.CANCEL) return;
		Object[] result = dialog.getResult();
		if (result == null || result.length == 0) return;
		final Path path = (Path)result[0];
		IProject selectedProject;
		File target = null;
		if (path.segmentCount() == 1) {
			selectedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(path.toString());
			target = selectedProject.getLocation().toFile();
		} else {
			IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			selectedProject = targetFile.getProject();
			target = targetFile.getLocation().toFile();
		}
		final IProject targetProject = selectedProject;
		final File destPath = target;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				ISVNClientAdapter client = null;
				ISVNRepositoryLocation repository = null;
				try {
					ISVNLocalResource svnTargetResource = SVNWorkspaceRoot.getSVNResourceFor(targetProject);					
					for (int i = 0; i < resources.length; i++) {
						final IResource resource = resources[i];
						if (client == null) {
							repository = SVNWorkspaceRoot.getSVNResourceFor(resources[i]).getRepository();
						    client = repository.getSVNClient();
						}
						File checkFile = new File(destPath.getPath() + File.separator + resource.getName());
						File srcPath = new File(resource.getLocation().toString());
						File newDestPath = new File(destPath.getPath() + File.separator + resource.getName());
						if (checkFile.exists()) {
							IInputValidator inputValidator = new IInputValidator() {
								public String isValid(String newText) {
									if (newText.equals(resource.getName())) 
										return Policy.bind("CopyAction.nameConflictSame"); //$NON-NLS-1$
									return null;
								}								
							};
							InputDialog inputDialog = new InputDialog(getShell(), Policy.bind("CopyAction.nameConflictTitle"), Policy.bind("CopyAction.nameConflictMessage", resource.getName()), "Copy of " + resource.getName(), inputValidator); //$NON-NLS-1$
							if (inputDialog.open() == InputDialog.CANCEL) return;
							String newName = inputDialog.getValue();
							if (newName == null  || newName.trim().length() == 0) return;
							newDestPath = new File(destPath.getPath() + File.separator + newName);
						}
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						boolean sameRepository = svnTargetResource != null && svnTargetResource.getRepository() != null && svnTargetResource.getRepository().getLocation().equals(svnResource.getRepository().getLocation());
						if (sameRepository)
							client.copy(srcPath, newDestPath);
						else
							client.doExport(srcPath, newDestPath, true);
						SVNUIPlugin.getPlugin().getRepositoryManager().resourceCreated(null, null);
					}
					targetProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("CopyAction.copy"), e.getMessage()); //$NON-NLS-1$
				}
				finally {
					if (repository != null) {
						repository.returnSVNClient(client);
					}
				}
			}			
		});
	}

	protected boolean isEnabled() throws TeamException {
		// Only enabled if all selections are from same project.
		boolean enabled = super.isEnabled();
		if (!enabled) return false;
		IResource[] resources = getSelectedResources();
		IProject project = null;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IProject) return false;
			if (project != null && !resources[i].getProject().equals(project)) return false;
			project = resources[i].getProject();
		}
		return true;
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_COPY;
	}
	
}
