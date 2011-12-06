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
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Checkout the remote resources into the local workspace as projects. Each
 * resource will be checked out into the corresponding project. You can use
 * getProject to get a project for a given remote Folder
 * 
 * Resources existing in the local file system at the target project location
 * but now known to the workbench will be overwritten.
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public class CheckoutCommand implements ISVNCommand {

	private ISVNRemoteFolder[] resources;

	private IProject[] projects;
	
	private IPath projectRoot;
	
	private SVNRevision svnRevision = SVNRevision.HEAD;
	
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean ignoreExternals = false;
    private boolean force = true;
	
	private boolean refreshProjects = true;
	
	private List<IProject> createProjectList = new ArrayList<IProject>();
	private List<IProject> manageProjectList = new ArrayList<IProject>();

	public CheckoutCommand(ISVNRemoteFolder[] resources, IProject[] projects) {
		this(resources, projects, null);
	}
	
	public CheckoutCommand(ISVNRemoteFolder[] resources, IProject[] projects, IPath root) {
		this.resources = resources;
		this.projects = projects;
		this.projectRoot = root;
	}

	protected void basicRun(final IProject project, ISVNRemoteFolder resource, final IProgressMonitor pm) throws SVNException {
		ISVNClientAdapter svnClient = null;
		if (pm != null)
		{
			pm.beginTask(null, 1000);
		}

		try {
			// Perform the checkout
			
			boolean createProject = false;

			svnClient = resource.getRepository().getSVNClient();
			OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(pm, svnClient));

			// Prepare the target projects to receive resources
			scrubProject(resource, project, (pm != null) ? Policy.subMonitorFor(pm, 100)
					: null);

			File destPath;
			if (project.getLocation() == null) {
				// project.getLocation is null if the project does
				// not exist in the workspace
				if (projectRoot==null) {
					ISVNLocalFolder root = SVNWorkspaceRoot.getSVNFolderFor(ResourcesPlugin
							.getWorkspace().getRoot());
					destPath = new File(root.getIResource().getLocation().toFile(),
							project.getName());
				} else {
					destPath = new File(projectRoot.toFile(), project.getName());					
				}
				if (!destPath.exists()) {
					destPath.mkdirs();
				}
				createProject = true;
			} else {
				if (projectRoot!=null) {
					try {
					destPath = new File(projectRoot.toFile(), project.getName());
					setProjectToRoot(project, destPath);
					} catch (CoreException e) {
						throw new SVNException(
								"Cannot create project to checkout to", e);
					} 
					
				} else {
					destPath = project.getLocation().toFile();
				}
			}
			
			if (createProject) {
				createProjectList.add(project);
			}

			checkoutProject(pm, resource, svnClient, destPath);

			SVNWorkspaceRoot.setManagedBySubclipse(project);
			if (refreshProjects) {
				try {
					project.create(null);
					project.open(null);
				} catch (CoreException e1) {
					throw new SVNException(
							"Cannot create project to checkout to", e1);
				}
				refreshProject(project, (pm != null) ? Policy.subMonitorFor(pm, 100) : null);
			}
			else {
				manageProjectList.add(project);
			}
		}finally {
			resource.getRepository().returnSVNClient(svnClient);
			if (pm != null) {
				pm.done();
			}
		}
	}

	public List<IProject> getCreateProjectList() {
		return createProjectList;
	}

	/**
	 * @return Returns the manageProjectList.
	 */
	public List<IProject> getManageProjectList() {
		return manageProjectList;
	}

	private void setProjectToRoot(final IProject project, File destPath) throws CoreException {
		IProjectDescription description = project.getDescription();
		description.setLocation(new Path(destPath.getAbsolutePath()));
		project.move(description, true, null);
	}
	
	/**
	 * @param pm
	 * @param resource
	 * @param svnClient
	 * @param destPath
	 * @throws SVNException
	 */
	private void checkoutProject(final IProgressMonitor pm, ISVNRemoteFolder resource, ISVNClientAdapter svnClient, File destPath) throws SVNException {
		final IProgressMonitor subPm = Policy.infiniteSubMonitorFor(pm, 800);
		try {
			subPm.beginTask("", Policy.INFINITE_PM_GUESS_FOR_CHECKOUT);
			
			// If checking out a specific revision, check to see if the location has changed in the
			// repository and adjust the URL if it has.
			SVNUrl url;
			if (svnRevision instanceof SVNRevision.Number) {
				url = Util.getUrlForRevision(resource, (SVNRevision.Number)svnRevision, subPm);
			}
			else {
				url = resource.getUrl();
			}
			
			svnClient.checkout(url, destPath, svnRevision, depth, ignoreExternals, force);
		} catch (SVNClientException e) {
			throw new SVNException("cannot checkout", e.operationInterrupted());
		} finally {
			subPm.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
		try {
			for (int i = 0; i < resources.length; i++) {
				final IProject project = projects[i]; 
				final ISVNRemoteFolder resource = resources[i]; 
				SVNProviderPlugin.run(new ISVNRunnable() {
					public void run(IProgressMonitor pm) throws SVNException {
						basicRun(project, resource, pm);
					} // run
				}, projects[i], Policy.monitorFor(monitor));
			}
		}finally {
			OperationManager.getInstance().endOperation();
		}
	}

	/*
	 * Delete the target projects before checking out
	 * @param monitor - may be null !
	 */
	private void scrubProject(ISVNRemoteFolder resource, IProject project, IProgressMonitor monitor)
			throws SVNException {
		if (project == null) {
			if (monitor !=null)
			{
				monitor.done();
			}
			return;
		}
		if (monitor != null)
		{
			monitor.beginTask("", 100);
			monitor.subTask(Policy.bind("SVNProvider.Scrubbing_local_project_1", project.getName())); //$NON-NLS-1$
		}
		try {
				File destPath = null;
				if (projectRoot != null) {
					destPath = new File(projectRoot.toFile(), project.getName());
				}	
				// New location, just delete the project but not the content.
				if (destPath != null && !destPath.exists() && project != null && project.exists()) {
					project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, monitor);
					project = null;
				}
				if (project != null && project.exists() && (destPath == null || destPath.exists())) {
					if (!project.isOpen()) {
						project.open((monitor != null) ? Policy.subMonitorFor(monitor, 10) : null);
					}
					// We do not want to delete the project to avoid a project
					// deletion delta
					// We do not want to delete the .project to avoid core
					// exceptions
					// unmap the project from any previous repository provider
					if (RepositoryProvider.getProvider(project) != null)
						RepositoryProvider.unmap(project);
					IResource[] children = project
							.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
					IProgressMonitor subMonitor = (monitor != null) ? Policy.subMonitorFor(monitor,
							80) : null;
					if (subMonitor != null)
					{
						subMonitor.beginTask(null, children.length * 100);
					}
					ISVNClientAdapter clientSilent = null;
					try {
						for (int j = 0; j < children.length; j++) {
							if (!children[j].getName().equals(".project")) {//$NON-NLS-1$
								if (clientSilent == null)
									clientSilent = SVNProviderPlugin.getPlugin().getSVNClient();
								ISVNInfo info = null;
								try {
									SVNUrl url = new SVNUrl(resource.getUrl().toString() + "/" + children[j].getProjectRelativePath());
									try {
								        SVNProviderPlugin.disableConsoleLogging(); 
										info = clientSilent.getInfo(url);
									} catch (SVNClientException e2) {
									} finally {
								        SVNProviderPlugin.enableConsoleLogging(); 
									}
								} catch (MalformedURLException e1) {
								}
								if (info != null)
									children[j].delete(true /* force */, (subMonitor != null) ? Policy
										.subMonitorFor(subMonitor, 100) : null);
							}
						}
					} finally {
						if (subMonitor != null)
						{
							subMonitor.done();
						}
						SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(clientSilent);
					}
				} else if (project != null) {
					// Make sure there is no directory in the local file system.
					File location = new File(project.getParent().getLocation()
							.toFile(), project.getName());
					if (location.exists()) {
						deepDelete(location);
					}
				}
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		} finally {
			if (monitor != null)
			{
				monitor.subTask(" ");
				monitor.done();
			}
		}
	}

	/*
	 * delete a folder recursively
	 */
	private void deepDelete(File resource) {
		if (resource.isDirectory()) {
			File[] fileList = resource.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				deepDelete(fileList[i]);
			}
		}
		resource.delete();
	}

	/*
	 * Bring the provided projects into the workspace
	 */
	private void refreshProject(IProject project, IProgressMonitor monitor)
			throws SVNException {
	    if (monitor != null)
	    {
	    	monitor.beginTask("", 100); //$NON-NLS-1$
	    	monitor.subTask(Policy.bind("SVNProvider.Creating_project_1", project.getName()));
	    }
		try {
			// Register the project with Team
			RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
			RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId());
		} catch (TeamException e) {
			throw new SVNException("Cannot map the project with svn provider",e);
		} finally {
			if (monitor != null)
			{
				monitor.subTask(" ");
				monitor.done();
			}
		}
	}

	public void setSvnRevision(SVNRevision svnRevision) {
		this.svnRevision = svnRevision;
	}

	public void setRefreshProjects(boolean refreshProjects) {
		this.refreshProjects = refreshProjects;
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