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

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.IPromptCondition;
import org.tigris.subversion.subclipse.ui.util.PromptingDialog;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Add some remote resources to the workspace. Current implementation:
 * -Works only for remote folders
 * -Does not prompt for project name; uses folder name instead
 */
public class CheckoutAsProjectAction extends SVNAction {


	/*
	 * @see SVNAction#execute()
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
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
					ISVNRemoteFolder[] folders = getSelectedRemoteFolders();
							
					List targetProjects = new ArrayList();
					Map targetFolders = new HashMap();

					monitor.beginTask(null, 100);
					for (int i = 0; i < folders.length; i++) {

						String name = folders[i].getName();
						
						// Check for a better name for the project
						try {
							ISVNResource[] children = folders[i].members(monitor, ISVNFolder.FILE_MEMBERS);
							for (int k = 0; k < children.length; k++) {
								ISVNResource resource = children[k];
								if(".project".equals(resource.getName())){
									ISVNRemoteFile dotProject = folders[0].getRepository().getRemoteFile(new SVNUrl(Util.appendPath(folders[i].getUrl().get(), ".project")));
																
									InputStream is = dotProject.getContents(monitor);
									DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
									org.w3c.dom.Document doc = db.parse(is);
									is.close();
									NodeList nl = doc.getDocumentElement().getChildNodes();
									for (int j = 0; j < nl.getLength(); ++j) {
										Node child = nl.item(j);
										if (child instanceof Element && "name".equals(child.getNodeName())) {
											Node grandChild = child.getFirstChild();
											if (grandChild instanceof Text) name = ((Text)grandChild).getData(); 	
										}
									}									
								}
							}

						}	
						catch (Exception e) {
						  // no .project exists ... that's ok
						}

						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
						targetFolders.put(name, folders[i]);
						targetProjects.add(project);
					}
					

					IResource[] projects = (IResource[]) targetProjects.toArray(new IResource[targetProjects.size()]);
					
					PromptingDialog prompt = new PromptingDialog(getShell(), projects, 
																  getOverwriteLocalAndFileSystemPrompt(), 
																  Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
					projects = prompt.promptForMultiple();
															
					if (projects.length != 0) {
						IProject[] localFolders = new IProject[projects.length];
						ISVNRemoteFolder[] remoteFolders = new ISVNRemoteFolder[projects.length];
						for (int i = 0; i < projects.length; i++) {
							localFolders[i] = (IProject)projects[i];
							remoteFolders[i] = (ISVNRemoteFolder)targetFolders.get(projects[i].getName());
						}
						
						monitor.setTaskName(getTaskName(remoteFolders));						
						SVNWorkspaceRoot.checkout(remoteFolders, localFolders, Policy.subMonitorFor(monitor, 100));
					}
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		}, true /* cancelable */, PROGRESS_DIALOG);
	}
		
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
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
                File localLocation  = getFileLocation(resource);
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


}
