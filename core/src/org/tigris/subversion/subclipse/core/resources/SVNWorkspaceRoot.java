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
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.DirEntry;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.sync.SVNRemoteSyncElement;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNUrl;


/**
 * This class provides static methods for checking out projects from a repository
 * into the local workspace and for converting IResources into SVNResources
 *
 *  Instances of this class represent a local workspace root (i.e. a project).
 */
public class SVNWorkspaceRoot {

	private ISVNLocalFolder localRoot;
    private SVNUrl url;
	
	public SVNWorkspaceRoot(IContainer resource){
		this.localRoot = getSVNFolderFor(resource);
	}

    /*
     * Delete the target projects before checking out
     */
    private static void scrubProjects(IProject[] projects, IProgressMonitor monitor) throws SVNException {
        if (projects == null) {
            monitor.done();
            return;
        }
        monitor.beginTask(Policy.bind("SVNProvider.Scrubbing_projects_1"), projects.length * 100); //$NON-NLS-1$
        try {   
            for (int i=0;i<projects.length;i++) {
                IProject project = projects[i];
                if (project != null && project.exists()) {
                    if(!project.isOpen()) {
                        project.open(Policy.subMonitorFor(monitor, 10));
                    }
                    // We do not want to delete the project to avoid a project deletion delta
                    // We do not want to delete the .project to avoid core exceptions
                    monitor.subTask(Policy.bind("SVNProvider.Scrubbing_local_project_1")); //$NON-NLS-1$
                    // unmap the project from any previous repository provider
                    if (RepositoryProvider.getProvider(project) != null)
                        RepositoryProvider.unmap(project);
                    IResource[] children = project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
                    IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 80);
                    subMonitor.beginTask(null, children.length * 100);
                    try {
                        for (int j = 0; j < children.length; j++) {
                            if ( ! children[j].getName().equals(".project")) {//$NON-NLS-1$
                                children[j].delete(true /*force*/, Policy.subMonitorFor(subMonitor, 100));
                            }
                        }
                    } finally {
                        subMonitor.done();
                    }
                } else if (project != null) {
                    // Make sure there is no directory in the local file system.
                    File location = new File(project.getParent().getLocation().toFile(), project.getName());
                    if (location.exists()) {
                        deepDelete(location);
                    }
                }
            }
        } catch (CoreException e) {
            throw SVNException.wrapException(e);
        } catch (TeamException e) {
            throw SVNException.wrapException(e);
        } finally {
            monitor.done();
        }
    }

    /*
     * delete a folder recursively
     */ 
    private static void deepDelete(File resource) {
        if (resource.isDirectory()) {
            File[] fileList = resource.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                deepDelete(fileList[i]);
            }
        }
        resource.delete();
    }

    /**
	 * Checkout the remote resources into the local workspace. Each resource will 
	 * be checked out into the corresponding project. 
	 * 
	 * Resources existing in the local file system at the target project location but now 
	 * known to the workbench will be overwritten.
	 */
	public static void checkout(
		final ISVNRemoteFolder[] resources,
		final IProject[] projects,
		final IProgressMonitor monitor)
		throws TeamException {
		final TeamException[] eHolder = new TeamException[1];
		try {

			IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor pm) throws CoreException {
					pm.beginTask(null, 1000 * resources.length);

					// Get the location of the workspace root
					ISVNLocalFolder root = SVNWorkspaceRoot.getSVNFolderFor(ResourcesPlugin.getWorkspace().getRoot());
                    
                    try {                        
                        // Prepare the target projects to receive resources
                        scrubProjects(projects, monitor);
                        pm.worked(100);
                        
                        for (int i = 0; i < resources.length; i++) {
                            IProject project = null;
                            RemoteFolder resource = (RemoteFolder) resources[i];

							project = projects[i];
							boolean deleteDotProject = false;
						    // Perform the checkout
                            SVNClientAdapter svnClient = resource.getRepository().getSVNClient();

							// check if the remote project has a .project file
							DirEntry[] rootFiles = svnClient.getList(resource.getUrl(), Revision.HEAD, false);
							for (int j = 0; j < rootFiles.length; j++) {
								if ((rootFiles[j].getNodeKind() == NodeKind.file) && (".project".equals(rootFiles[j].getPath()))) {
										deleteDotProject = true;
								}
							}							

                            File destPath;
							if (project.getLocation() == null) {
                                // project.getLocation is null if the project does not exist in the workspace
								destPath = new File(root.getIResource().getLocation().toFile(),project.getName());
                                // we create the directory corresponding to the project and we open it 
                                project.create(null);
                                project.open(null);

								if(deleteDotProject){
									
									IFile projectFile = project.getFile(".project");
									if (projectFile != null) {
										// delete the project file, force, no history, without progress monitor
										projectFile.delete(true, false, null);
									}
								}     
							} else {
								destPath = project.getLocation().toFile();
							}
							
                            OperationManager operationHandler = OperationManager.getInstance();
							try {
								operationHandler.beginOperation(svnClient);
								svnClient.checkout(resource.getUrl(), destPath, Revision.HEAD, true);
                                pm.worked(800); 
							} catch (ClientException e) {
								throw new SVNException("cannot checkout");
							} finally {
								operationHandler.endOperation(); 
							}

							// Bring the project into the workspace
							refreshProjects(
								new IProject[] { project },
								Policy.subMonitorFor(pm, 100));
                        } //for
					} catch (TeamException e) {
						// Pass it outside the workspace runnable
						eHolder[0] = e;
					} catch (ClientException ce) {
						eHolder[0] = new TeamException("Error Getting Dir list", ce);
					} finally {
						pm.done();
					}
				} // run
			};
			ResourcesPlugin.getWorkspace().run(workspaceRunnable, monitor);
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
		}
		// Re-throw the TeamException, if one occurred
		if (eHolder[0] != null) {
			throw eHolder[0];
		}
	}
					
	/**
	 * Create a remote directory in the SVN repository and link the project directory to this remote directory.
	 * The contents of the project are not imported.
     * if remoteDirName is null, the name of the project is used
     * if location is not in repositories, it is added 
	 */
	public static void shareProject(final ISVNRepositoryLocation location, final IProject project, String remoteDirName, IProgressMonitor monitor) throws TeamException {
		
		// Determine if the repository is known
		boolean alreadyExists = SVNProviderPlugin.getPlugin().getRepositories().isKnownRepository(location.getLocation());
		
        // Set the folder sync info of the project to point to the remote module
		final ISVNLocalFolder folder = (ISVNLocalFolder)SVNWorkspaceRoot.getSVNResourceFor(project);
			
		try {
			// Get the import properties
			String projectName = project.getName();
			if (remoteDirName == null)
				remoteDirName = projectName;


            final SVNClientAdapter svnClient = location.getSVNClient();
		
			// perform the workspace modifications in a runnable
			try {
				final TeamException[] exception = new TeamException[] {null};
				final String dirName = remoteDirName;
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						try {
                            String message = Policy.bind("SVNProvider.initialImport"); //$NON-NLS-1$
                            
                            try {
                                // create the remote dir
                                SVNUrl url = new SVNUrl(Util.appendPath(location.getUrl().toString(),dirName));
                                svnClient.mkdir(url,message);
                                
                                // checkout it so that we have .svn
                                svnClient.checkout(url,project.getLocation().toFile(),Revision.HEAD,false);
                            } catch (ClientException e) {
                                throw new SVNException("Error while creating module");  
                            } catch (MalformedURLException e) {
                                throw new SVNException("Error while creating module");
                            }
                                                       
							//Register it with Team.  If it already is, no harm done.
							RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
						} catch (TeamException e) {
							exception[0] = e;
						}
					}
				}, monitor);
				if (exception[0] != null)
					throw exception[0];
			} catch (CoreException e) {
				throw SVNException.wrapException(e);
			}
		} catch (TeamException e) {
			// The checkout may have triggered password caching
			// Therefore, if this is a newly created location, we want to clear its cache
			if ( ! alreadyExists)
				SVNProviderPlugin.getPlugin().getRepositories().disposeRepository(location);
			throw e;
		}
		// Add the repository if it didn't exist already
		if ( ! alreadyExists)
			SVNProviderPlugin.getPlugin().getRepositories().addRepository(location);
	}
	
	/**
	 * Set the sharing for a project to enable it to be used with the SVNTeamProvider.
     * This is used when a project has .svn directory but is not shared in Eclipse.
	 * An exception is thrown if project does not have a remote directory counterpart
	 */
	public static void setSharing(IProject project, IProgressMonitor monitor) throws TeamException {
		
		// Ensure provided info matches that of the project
		ISVNLocalFolder folder = (ISVNLocalFolder)SVNWorkspaceRoot.getSVNResourceFor(project);
		Status status = folder.getStatus();
        
        // this folder needs to be managed but also to have a remote counter-part
        // because we need to know its url
        // we will change this exception !
        if (!status.hasRemote())
            throw new SVNException(new SVNStatus(SVNStatus.ERROR, Policy.bind("SVNProvider.infoMismatch", project.getName())));//$NON-NLS-1$
        
		// Ensure that the provided location is managed
		ISVNRepositoryLocation location = SVNProviderPlugin.getPlugin().getRepositories().getRepository(status.getUrl().toString());
		
		// Register the project with Team
		RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
	}

	
	/*
	 * Bring the provided projects into the workspace
	 */
	private static void refreshProjects(IProject[] projects, IProgressMonitor monitor) throws CoreException, TeamException {
		monitor.beginTask(Policy.bind("SVNProvider.Creating_projects_2"), projects.length * 100); //$NON-NLS-1$
		try {
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				// Register the project with Team
				RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
				SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId());
			}
		} finally {
			monitor.done();
		}
	}
				
    /**
     * get the SVNLocalFolder for the given resource 
     */           	
	public static ISVNLocalFolder getSVNFolderFor(IContainer resource) {
		return new LocalFolder(resource);
	}

    /**
     * get the SVNLocalFile for the given resource 
     */
	public static ISVNLocalFile getSVNFileFor(IFile resource) {
		return new LocalFile(resource);
	}

    /**
     * get the SVNLocalResource for the given resource 
     */
	public static ISVNLocalResource getSVNResourceFor(IResource resource) {
		if (resource.getType() == IResource.FILE)
			return getSVNFileFor((IFile) resource);
		else // container
			return getSVNFolderFor((IContainer) resource);
	}
	
	public static ISVNRemoteResource getRemoteResourceFor(IResource resource) throws SVNException {
		ISVNLocalResource managed = getSVNResourceFor(resource);
		return managed.getRemoteResource();
	}
	
    /**
     * get the remotesync for the given resource.
     * A remotesync element is (managed,base,remote)  
     */
	public static IRemoteSyncElement getRemoteSync(IResource resource, IProgressMonitor progress) throws TeamException {
        ISVNLocalResource managed = SVNWorkspaceRoot.getSVNResourceFor(resource);
//        ISVNRemoteResource remote = managed.getRemoteResource();
//        ISVNRemoteResource base = managed.getLatestRemoteResource();
        ISVNRemoteResource remote = managed.getLatestRemoteResource();
        ISVNRemoteResource base = managed.getRemoteResource();
		return new SVNRemoteSyncElement(true /*three way*/, resource, base, remote);
	}

	/**
     * get the repository for this project 
	 */
	public ISVNRepositoryLocation getRepository() throws SVNException {
		if (url == null)
        {
            Status status = localRoot.getStatus();
            if (!status.isManaged()) {
                throw new SVNException(Policy.bind("SVNWorkspaceRoot.notSVNFolder", localRoot.getName()));  //$NON-NLS-1$
            }
            try {
                url = new SVNUrl(status.getUrl());
            } catch (MalformedURLException e) {
                throw SVNException.wrapException(e);
            }
        }
		return SVNProviderPlugin.getPlugin().getRepository(url.toString());
	}

    /**
     * get the svn folder corresponding to the project  
     */
	public ISVNLocalFolder getLocalRoot() {
		return localRoot;
	}
	
	/**
	 * Return true if the resource is part of a link (i.e. a linked resource or
	 * one of it's children.
	 * 
	 * @param container
	 * @return boolean
	 */
	public static boolean isLinkedResource(IResource resource) {
		// check the resource directly first
		if (resource.isLinked()) return true;
		// projects and root cannot be links
		if (resource.getType() == IResource.PROJECT || resource.getType() == IResource.ROOT) {
			return false;
		}
		// look one level under the project to see if the resource is part of a link
		String linkedParentName = resource.getProjectRelativePath().segment(0);
		IFolder linkedParent = resource.getProject().getFolder(linkedParentName);
		return linkedParent.isLinked();
	}
	
}
