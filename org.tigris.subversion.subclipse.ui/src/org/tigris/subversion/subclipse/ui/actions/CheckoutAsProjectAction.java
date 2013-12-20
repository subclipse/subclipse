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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.WorkspacePathValidator;
import org.tigris.subversion.subclipse.ui.operations.CheckoutAsProjectOperation;
import org.tigris.subversion.subclipse.ui.util.IPromptCondition;
import org.tigris.subversion.subclipse.ui.util.PromptingDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Add some remote resources to the workspace. Current implementation:
 * -Works only for remote folders
 * -Does not prompt for project name; uses folder name instead
 */
public class CheckoutAsProjectAction extends WorkspaceAction {
    protected IProject[] localFolders;
    protected ISVNRemoteFolder[] remoteFolders;
    protected IResource[] projects;
    protected boolean proceed;
	private ISVNRemoteFolder[] selectedFolders;
	private String projectName;
	private String projectNamePrefix;
	private String projectNameSuffix;
	private SVNRevision svnRevision = SVNRevision.HEAD;
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean ignoreExternals = false;
    private boolean force = true;
    private IWorkingSet[] workingSets;

	public CheckoutAsProjectAction() {
		super();
	}

	public CheckoutAsProjectAction(ISVNRemoteFolder[] selectedFolders, String projectName, Shell shell) {
		super();
		this.selectedFolders = selectedFolders;
		this.projectName = projectName;
		this.shell = shell;
	}

    public CheckoutAsProjectAction(ISVNRemoteFolder[] selectedFolders, String projectNamePrefix, String projectNameSuffix, Shell shell) {
        this(selectedFolders, null, shell);
        this.projectNamePrefix = projectNamePrefix;
        this.projectNameSuffix = projectNameSuffix;
    }

	public void setWorkingSets(IWorkingSet[] workingSets) {
		this.workingSets = workingSets;
	}

	/*
	 * @see SVNAction#execute()
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
	    if (!WorkspacePathValidator.validateWorkspacePath()) return;
	    checkoutSelectionIntoWorkspaceDirectory();
	}

    /**
     * checkout into a workspace directory, ie as a project
     * @throws InvocationTargetException
     * @throws InterruptedException
     */
	protected void checkoutSelectionIntoWorkspaceDirectory() throws InvocationTargetException, InterruptedException {
	    run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
			    try {
					final ISVNRemoteFolder[] folders = getSelectedRemoteFolders();
					final boolean renameMultipleProjects = shouldRenameMultipleProjects();
					List targetProjects = new ArrayList();
					Map targetFolders = new HashMap();

					monitor.beginTask(null, 100);
					for (int i = 0; i < folders.length; i++) {
					    proceed = true;
					    if (folders[i].getRepository().getRepositoryRoot().toString().equals(folders[i].getUrl().toString())) {
						    shell.getDisplay().syncExec(new Runnable() {
	                            public void run() {
	        					     proceed = MessageDialog.openQuestion(shell, Policy.bind("CheckoutAsProjectAction.title"), Policy.bind("AddToWorkspaceAction.checkingOutRoot")); //$NON-NLS-1$
	                            }
						    });
					    }
					    if (proceed) {
					    	IProject project;
					    	if (renameMultipleProjects) {
					    	    String originalProjectName;
					    	    try {
					    	        originalProjectName = SVNWorkspaceRoot.getProjectName(folders[i], monitor);
					    	    } catch (Exception e) {
					    	        originalProjectName = folders[i].getName();
					    	    }
					    	    final String newProjectName = modifyProjectName(originalProjectName);
					    	    project = SVNWorkspaceRoot.getProject(newProjectName);
					    	} else if (projectName == null) {
					    		try {
					    			project = SVNWorkspaceRoot.getProject(folders[i],monitor);
					    		} catch (Exception e) {
					    			project = SVNWorkspaceRoot.getProject(folders[i].getName());
					    		}
					    	} else {
					    		project = SVNWorkspaceRoot.getProject(projectName);
					    	}
							targetFolders.put(project.getName(), folders[i]);
							targetProjects.add(project);
					    } else return;
					}


					projects = (IResource[]) targetProjects.toArray(new IResource[targetProjects.size()]);

					// if a project with the same name already exist, we ask the user if he want to overwrite it
					PromptingDialog prompt = new PromptingDialog(getShell(), projects,
																  getOverwriteLocalAndFileSystemPrompt(),
																  Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
					projects = prompt.promptForMultiple();

					if (projects.length != 0) {
						localFolders = new IProject[projects.length];
						remoteFolders = new ISVNRemoteFolder[projects.length];
						for (int i = 0; i < projects.length; i++) {
							localFolders[i] = (IProject)projects[i];
							remoteFolders[i] = (ISVNRemoteFolder)targetFolders.get(projects[i].getName());
						}
					} else
					    proceed = false;
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		}, true /* cancelable */, PROGRESS_DIALOG);
	    if (proceed) {
	    	CheckoutAsProjectOperation checkoutAsProjectOperation = new CheckoutAsProjectOperation(getTargetPart(), remoteFolders, localFolders);
	    	checkoutAsProjectOperation.setSvnRevision(svnRevision);
	    	checkoutAsProjectOperation.setDepth(depth);
	    	checkoutAsProjectOperation.setIgnoreExternals(ignoreExternals);
	    	checkoutAsProjectOperation.setForce(force);
	    	checkoutAsProjectOperation.setWorkingSets(workingSets);
	    	checkoutAsProjectOperation.run();
	    }
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
		ISVNRemoteFolder[] resources = getSelectedRemoteFolders();
		if (resources.length == 0) return false;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof ISVNRepositoryLocation) return false;
		}
		return true;
	}
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("AddToWorkspaceAction.checkoutFailed"); //$NON-NLS-1$
	}

    protected static String getTaskName(ISVNRemoteFolder[] remoteFolders) {
        if (remoteFolders.length == 1) {
            ISVNRemoteFolder folder = remoteFolders[0];
            String label = folder.getRepositoryRelativePath();
            return Policy.bind("AddToWorkspace.taskName1", label);  //$NON-NLS-1$
        }
        else {
            return Policy.bind("AddToWorkspace.taskNameN", new Integer(remoteFolders.length).toString());  //$NON-NLS-1$
        }
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
                getFileLocation(resource);
                if(resource.exists()) {
                    return Policy.bind("AddToWorkspaceAction.thisResourceExists", resource.getName());//$NON-NLS-1$
                } else {
                    return Policy.bind("AddToWorkspaceAction.thisExternalFileExists", resource.getName());//$NON-NLS-1$
                }
            }
            private File getFileLocation(IResource resource) {
                return new File(resource.getParent().getLocation().toFile(), resource.getName());
            }
        };
    }

	protected ISVNRemoteFolder[] getSelectedRemoteFolders() {
		if (selectedFolders != null) return selectedFolders;
		return super.getSelectedRemoteFolders();
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

    private String getProjectNamePrefix() {
        return projectNamePrefix;
    }

    private String getProjectNameSuffix() {
        return projectNameSuffix;
    }

    /**
     * @return <code>true</code>, if multiple projects should be checked out and project name
     * prefix and/or suffix is specified
     */
    private boolean shouldRenameMultipleProjects() {
        final ISVNRemoteFolder[] folders = getSelectedRemoteFolders();
        return ((folders != null) && (folders.length > 1)
                && ((getProjectNamePrefix() != null) || (getProjectNameSuffix() != null)));
    }

    /**
     * Modifies the original project name to have an optional prefix and/or suffix.
     *
     * @param originalProjectName the project name to modify
     * @return the project name with optional prefix and/or suffix
     */
    private String modifyProjectName(String originalProjectName) {
        final StringBuffer buffer = new StringBuffer();
        if (getProjectNamePrefix() != null) {
            buffer.append(getProjectNamePrefix().trim());
        }
        buffer.append(originalProjectName);
        if (getProjectNameSuffix() != null) {
            buffer.append(getProjectNameSuffix().trim());
        }
        return buffer.toString();
    }
}
