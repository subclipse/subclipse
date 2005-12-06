/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
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

	public CheckoutCommand(ISVNRemoteFolder[] resources, IProject[] projects) {
		this(resources, projects, null);
	}
	
	public CheckoutCommand(ISVNRemoteFolder[] resources, IProject[] projects, IPath root) {
		this.resources = resources;
		this.projects = projects;
		this.projectRoot = root;
	}

	protected void basicRun(final IProject project, ISVNRemoteFolder resource, final IProgressMonitor pm) throws SVNException {
		if (pm != null)
		{
			pm.beginTask(null, 1000);
		}

		try {
			// Perform the checkout
			ISVNClientAdapter svnClient = resource.getRepository()
					.getSVNClient();

			// Prepare the target projects to receive resources
			scrubProject(resource, project, (pm != null) ? Policy.subMonitorFor(pm, 100)
					: null);

			boolean deleteDotProject = false;

			// check if the remote project has a .project file
			ISVNDirEntry[] rootFiles = svnClient.getList(resource.getUrl(),
					SVNRevision.HEAD, false);
			for (int j = 0; j < rootFiles.length; j++) {
				if ((rootFiles[j].getNodeKind() == SVNNodeKind.FILE)
						&& (".project".equals(rootFiles[j].getPath()))) {
					deleteDotProject = true;
				}
			}

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
				try {
					// we create the directory corresponding to the
					// project and we open it
					
					project.create(null);
					project.open(null);
					if (projectRoot!=null) {
						setProjectToRoot(project, destPath);
					}
				} catch (CoreException e1) {
					throw new SVNException(
							"Cannot create project to checkout to", e1);
				}

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

			//delete the project file if the flag gets set.
			//fix for 54
			if (deleteDotProject) {

				IFile projectFile = project.getFile(".project");
				if (projectFile != null) {
					try {
						// delete the project file, force, no history,
						// without progress monitor
						projectFile.delete(true, false, null);
					} catch (CoreException e1) {
						throw new SVNException(
								"Cannot delete .project before checkout", e1);
					}
				}
			}

			checkoutProject(pm, resource, svnClient, destPath);

			// Bring the project into the workspace
			refreshProject(project, (pm != null) ? Policy
					.subMonitorFor(pm, 100) : null);
		} catch (SVNClientException ce) {
			throw new SVNException("Error Getting Dir list", ce);
		} finally {
			if (pm != null) {
				pm.done();
			}
		}
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
//			subPm.setTaskName("");
			OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(subPm));
			svnClient.checkout(resource.getUrl(), destPath, SVNRevision.HEAD, true);
		} catch (SVNClientException e) {
			throw new SVNException("cannot checkout");
		} finally {
			OperationManager.getInstance().endOperation();
			subPm.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
		for (int i = 0; i < resources.length; i++) {
			final IProject project = projects[i]; 
			final ISVNRemoteFolder resource = resources[i]; 
			SVNProviderPlugin.run(new ISVNRunnable() {
				public void run(IProgressMonitor pm) throws SVNException {
					basicRun(project, resource, pm);
				} // run
			}, projects[i], Policy.monitorFor(monitor));
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
				if (project != null && project.exists()) {
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
					try {
						ISVNClientAdapter clientSilent = null;
						for (int j = 0; j < children.length; j++) {
							if (!children[j].getName().equals(".project")) {//$NON-NLS-1$
								if (clientSilent == null)
									clientSilent = SVNProviderPlugin.getPlugin().createSVNClient();
								ISVNInfo info = null;
								try {
									SVNUrl url = new SVNUrl(resource.getUrl().toString() + "/" + children[j].getProjectRelativePath());
									try {
										info = clientSilent.getInfo(url);
									} catch (SVNClientException e2) {
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

}