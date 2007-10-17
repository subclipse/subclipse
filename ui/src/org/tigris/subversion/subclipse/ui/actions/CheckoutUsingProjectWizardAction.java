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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.WorkspacePathValidator;
import org.tigris.subversion.subclipse.ui.operations.CheckoutAsProjectOperation;
import org.tigris.subversion.subclipse.ui.util.IPromptCondition;
import org.tigris.subversion.subclipse.ui.util.PromptingDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Add a remote resource to the workspace. Current implementation:
 * - Works only for remote folders
 * - prompt for project name
 */
public class CheckoutUsingProjectWizardAction extends SVNAction {
	ISVNRemoteFolder[] folders;
	private SVNRevision svnRevision = SVNRevision.HEAD;
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean ignoreExternals = false;
    private boolean force = true;	

    public CheckoutUsingProjectWizardAction(ISVNRemoteFolder[] folders) {
		super();
		this.folders = folders;
	}

	/*
     * @see TeamAction#isEnabled()
     */
    protected boolean isEnabled() {
//        return getSelectedRemoteFolders().length == 1;
    	return folders.length > 0;
    }

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		if (!WorkspacePathValidator.validateWorkspacePath()) return;
	    
		if (folders.length == 1){
			if (folders[0].getRepository().getRepositoryRoot().toString().equals(folders[0].getUrl().toString())) {
			    if (!MessageDialog.openQuestion(shell, Policy.bind("CheckoutAsAction.title"), Policy.bind("AddToWorkspaceAction.checkingOutRoot"))) return; //$NON-NLS-1$
			}
			checkoutSingleProject(folders[0]);
		}
	}
	
    /**
     * checkout a remote folder as a project
     */
	private void checkoutSingleProject(final ISVNRemoteFolder remoteFolder) throws InvocationTargetException, InterruptedException {
		// Fetch the members of the folder to see if they contain a .project file.
		final String remoteFolderName = remoteFolder.getName();
		final boolean[] hasProjectMetaFile = new boolean[] { false };
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					remoteFolder.members(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}

			}
		}, true /* cancelable */, PROGRESS_DIALOG);
		
		// Prompt outside a workspace runnable so that the project creation delta can be heard
		IProject newProject = null;
		IProjectDescription newDesc = null;
		if (hasProjectMetaFile[0]) {

			
		} else {
			newProject = getNewProject(remoteFolderName);
			if (newProject == null) return;
		}
		
		final IProject project = newProject;
		final IProjectDescription desc = newDesc;
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					monitor.beginTask(null, 100);
					monitor.setTaskName(Policy.bind("CheckoutAsAction.taskname", remoteFolderName, project.getName())); //$NON-NLS-1$
					int used = 0;
					if (hasProjectMetaFile[0]) {
						used = 5;
						createAndOpenProject(project, desc, Policy.subMonitorFor(monitor, used));
					}
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		}, true /* cancelable */, PROGRESS_DIALOG);
		CheckoutAsProjectOperation checkoutAsProjectOperation = new CheckoutAsProjectOperation(getTargetPart(), new ISVNRemoteFolder[] { remoteFolder }, new IProject[] { project });
		checkoutAsProjectOperation.setSvnRevision(svnRevision);
    	checkoutAsProjectOperation.setDepth(depth);
    	checkoutAsProjectOperation.setIgnoreExternals(ignoreExternals);
    	checkoutAsProjectOperation.setForce(force);	  		
		checkoutAsProjectOperation.run();
	}

	
    /**
     * Creates a project and open it 
     */
	private void createAndOpenProject(IProject project, IProjectDescription desc, IProgressMonitor monitor) throws SVNException {
		try {
			monitor.beginTask(null, 5);
			if (project.exists()) {
				if (desc != null) {
					project.move(desc, true, Policy.subMonitorFor(monitor, 3));
				}
			} else {
				if (desc == null) {
					// create in default location
					project.create(Policy.subMonitorFor(monitor, 3));
				} else {
					// create in some other location
					project.create(desc, Policy.subMonitorFor(monitor, 3));
				}
			}
			if (!project.isOpen()) {
				project.open(Policy.subMonitorFor(monitor, 2));
			}
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Get a new project.
	 * 
	 * The suggestedName is not currently used but is a desired capability.
	 */
	private IProject getNewProject(String suggestedName) {
		NewProjectListener listener = new NewProjectListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		(new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow())).run();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		// Ensure that the project only has a single member which is the .project file
		IProject project = listener.getNewProject();
		if (project == null) return null;
		try {
			IResource[] members = project.members();
			if ((members.length == 0) 
				||(members.length == 1 && members[0].getName().equals(".project"))) { //$NON-NLS-1$
				return project;
			} else {
				// prompt to overwrite
				PromptingDialog prompt = new PromptingDialog(getShell(), 
						new IProject[] { project }, 
						CheckoutUsingProjectWizardAction.getOverwriteLocalAndFileSystemPrompt(), 
						Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
				try {
					if (prompt.promptForMultiple().length == 1) return project;
				} catch (InterruptedException e) {
				}
			}
		} catch (CoreException e) {
			handle(e);
		}
		return null;
	}
	
    /**
     * Listener used to get the project  by NewProjectAction
     */
	class NewProjectListener implements IResourceChangeListener {
		private IProject newProject = null;
		/**
		 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
		 */
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta root = event.getDelta();
			IResourceDelta[] projectDeltas = root.getAffectedChildren();
			for (int i = 0; i < projectDeltas.length; i++) {							
				IResourceDelta delta = projectDeltas[i];
				IResource resource = delta.getResource();
				if (delta.getKind() == IResourceDelta.ADDED) {
					newProject = (IProject)resource;
				}
			}
		}
		/**
		 * Gets the newProject.
		 * @return Returns a IProject
		 */
		public IProject getNewProject() {
			return newProject;
		}
	}
    
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("CheckoutAsAction.checkoutFailed"); //$NON-NLS-1$
	}
	
    /**
     * get an IPromptCondition 
     */
    static public IPromptCondition getOverwriteLocalAndFileSystemPrompt() {
        return new IPromptCondition() {
            // prompt if resource in workspace exists or exists in local file system
            public boolean needsPrompt(IResource resource) {
                File localLocation  = getFileLocation(resource);
                if(resource.exists() || localLocation.exists()) {
                    return true;
                }
                return false;
            }
            public String promptMessage(IResource resource) {
                return Policy.bind("CheckoutAsAction.confirmOverwrite", resource.getName());//$NON-NLS-1$
            }
            private File getFileLocation(IResource resource) {
                return new File(resource.getParent().getLocation().toFile(), resource.getName());
            }
        };
    }

	public void setSvnRevision(SVNRevision svnRevision) {
		this.svnRevision = svnRevision;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setIgnoreExternals(boolean ignoreExternals) {
		this.ignoreExternals = ignoreExternals;
	}

	public void setForce(boolean force) {
		this.force = force;
	}					

}
