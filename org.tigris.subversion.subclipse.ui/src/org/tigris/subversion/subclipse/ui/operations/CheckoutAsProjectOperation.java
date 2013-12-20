/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.operations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CheckoutAsProjectOperation extends SVNOperation {
    private ISVNRemoteFolder[] remoteFolders;
    private IProject[] localFolders;
    private IPath projectRoot;
    private SVNRevision svnRevision = SVNRevision.HEAD;
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean ignoreExternals = false;
    private boolean force = true;
    private List<IProject> createProjectList = new ArrayList<IProject>();
    private List<IProject> manageProjectList = new ArrayList<IProject>();
    private IWorkingSet[] workingSets;

    public CheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders) {
    	this(part, remoteFolders, localFolders, null);
    }
    
    public CheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders, IPath projectRoot) {
        super(part);
        this.remoteFolders = remoteFolders;
        this.localFolders = localFolders;
        this.projectRoot = projectRoot; 
    }
    
    public void setWorkingSets(IWorkingSet[] workingSets) {
		this.workingSets = workingSets;
	}

	protected String getTaskName() {
        return Policy.bind("CheckoutAsProjectOperation.taskName"); //$NON-NLS-1$;
    }
    
	protected ISchedulingRule getSchedulingRule(SVNTeamProvider provider) {
		IResourceRuleFactory ruleFactory = provider.getRuleFactory();
		HashSet rules = new HashSet();
		for (int i = 0; i < localFolders.length; i++) {
			rules.add(ruleFactory.modifyRule(localFolders[i].getProject()));
		}
		return MultiRule.combine((ISchedulingRule[]) rules.toArray(new ISchedulingRule[rules.size()]));
	}
	
	private void createProject(final IProject project) throws SVNException {
		try {
			IProject newProject;
			if (projectRoot == null) {
				project.create(null);
				project.open(null);
				newProject = project;
			}
			else {
				String path = projectRoot.toString();
				if (!path.endsWith("/")) {
					path = path + "/";
				}
				IProjectDescription description;
				try {
					description = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(path + project.getName() + "/.project"));
				} catch (CoreException e) {
					description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
					description.setLocation(new Path(path + project.getName()));
				}
				IProject customProject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
				customProject.create(description, null);
				customProject.open(null);
				newProject = customProject;
			}
			if (workingSets != null && workingSets.length > 0) {
				// Have to use reflection for compatibility with Eclipse 3.2 API
				IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
				Class[] parameterTypes = { IAdaptable.class, IWorkingSet[].class };
				Method addToWorkingSets = null;
				try {
					addToWorkingSets = manager.getClass().getMethod("addToWorkingSets", parameterTypes);
					if (addToWorkingSets != null) {
						addToWorkingSets.invoke(manager, newProject, workingSets);
					}					
				} catch (Exception e) {}
			}
		} catch (CoreException e1) {
			throw new SVNException(
					"Cannot create project to checkout to", e1);
		}
	}

    public void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
    	// First checkout all projects, then bring them into workspace.
//    	List failedProjects = new ArrayList();
        monitor.beginTask(null, remoteFolders.length * 1000);
        for (int i = 0; i < remoteFolders.length; i++) {
            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			try {
				monitor.setTaskName(Policy.bind("CheckoutAsProjectOperation.0", remoteFolders[i].getName())); //$NON-NLS-1$
				IProject[] local = new IProject[1];
				local[0] = localFolders[i];
				ISVNRemoteFolder[] remote = new ISVNRemoteFolder[1];
				remote[0] = remoteFolders[i];
				execute(remote, local, subMonitor);
				if (monitor.isCanceled()) {
					break;
				}
			} finally {}
        }
        for (IProject project : createProjectList) {
        	createProject(project);
        }
        for (IProject project : manageProjectList) {
        	IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			try {
				monitor.setTaskName(Policy.bind("SVNProvider.Creating_project_1", project.getName())); //$NON-NLS-1$
				refreshProject(project, subMonitor);
			} finally {}
        }
    }
    
    protected boolean execute(ISVNRemoteFolder[] remote, IProject[] local, IProgressMonitor monitor) throws SVNException, InterruptedException {
    	try {
			CheckoutCommand command;
			if (projectRoot==null) {
				command = new CheckoutCommand(remote, local);
			} else {
				command = new CheckoutCommand(remote, local, projectRoot);
			}
			command.setSvnRevision(svnRevision);
			command.setDepth(depth);
			command.setIgnoreExternals(ignoreExternals);
			command.setForce(force);
			command.setRefreshProjects(false);
	    	command.run(monitor);
	    	List<IProject> commandCreateProjectList = command.getCreateProjectList();
	    	for (IProject project : commandCreateProjectList) {
	    		createProjectList.add(project);
	    	}
	    	List<IProject> commandManageProjectList = command.getManageProjectList();
	    	for (IProject project : commandManageProjectList) {
	    		manageProjectList.add(project);
	    	}
		} catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
			return false;
		}
		return true;
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
	
	/*
	 * Bring the provided projects into the workspace
	 */
	private void refreshProject(IProject project, IProgressMonitor monitor)
			throws SVNException {
	    if (monitor != null)
	    {
	    	monitor.beginTask("", 100); //$NON-NLS-1$
	    }
		try {
			// Register the project with Team
			RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
			RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId());
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (Exception e) {
			throw new SVNException("Cannot map the project with svn provider",e);
		} finally {
			if (monitor != null)
			{
				monitor.done();
			}
		}
	}	
    
}
