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
package org.tigris.subversion.subclipse.core;
 
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.SVNMoveDeleteHook;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

import com.qintsoft.jsvn.jni.ClientException;
import com.qintsoft.jsvn.jni.Revision;

/**
 * This class is responsible for configuring a project for repository management
 * and providing the necessary hooks for resource modification
 */
public class SVNTeamProvider extends RepositoryProvider {
	private SVNWorkspaceRoot workspaceRoot;
	private IProject project;
	
	/**
	 * No-arg Constructor for IProjectNature conformance
	 */
	public SVNTeamProvider() {
	}

	/**
	 * @see IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {

	}
	
    /**
     * @see RepositoryProvider#deconfigured()
     */
	public void deconfigured() {
		SVNProviderPlugin.broadcastProjectDeconfigured(getProject());
	}


	/**
	 * @see IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * @see IProjectNature#setProject(IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
		try {
			this.workspaceRoot = new SVNWorkspaceRoot(project);
			// Ensure that the project has SVN info
			if (!workspaceRoot.getLocalRoot().hasRemote()) {
				throw new SVNException(new SVNStatus(SVNStatus.ERROR, Policy.bind("SVNTeamProvider.noFolderInfo", project.getName()))); //$NON-NLS-1$
			}
		} catch (SVNException e) {
			// Log any problems creating the CVS managed resource
			SVNProviderPlugin.log(e);
		}
	}

	/**
	 * Add the given resources to the project. 
	 * <p>
	 * The sematics follow that of SVN in the sense that any folders and files
	 * are created remotely on the next commit. 
	 * </p>
	 */
	public void add(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException {	
		
		// Visit the children of the resources using the depth in order to
		// determine which folders, text files and binary files need to be added
		// A TreeSet is needed for the folders so they are in the right order (i.e. parents created before children)
		final SortedSet folders = new TreeSet();
		// Sets are required for the files to ensure that files will not appear twice if there parent was added as well
		// and the depth isn't zero
		final HashSet files = new HashSet();
		final TeamException[] eHolder = new TeamException[1];
		
        
        for (int i=0; i<resources.length; i++) {
			
			final IResource currentResource = resources[i];
			
			try {		
				// Auto-add parents if they are not already managed
				IContainer parent = currentResource.getParent();
				ISVNLocalResource svnParentResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
				while (parent.getType() != IResource.ROOT && parent.getType() != IResource.PROJECT && ! svnParentResource.isManaged()) {
					folders.add(svnParentResource);
					parent = parent.getParent();
					svnParentResource = svnParentResource.getParent();
				}
					
				// Auto-add children accordingly to depth
				final TeamException[] exception = new TeamException[] { null };
				currentResource.accept(new IResourceVisitor() {
					public boolean visit(IResource resource) {
						try {
							ISVNLocalResource mResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
							// Add the resource is its not already managed and it was either
							// added explicitly (is equal currentResource) or is not ignored
							if ((! mResource.isManaged()) && (currentResource.equals(resource) || ! mResource.isIgnored())) {
								if (resource.getType() == IResource.FILE) {
									files.add(mResource);
								} else {
									folders.add(mResource);
								}
							}
							// Always return true and let the depth determine if children are visited
							return true;
						} catch (SVNException e) {
							exception[0] = e;
							return false;
						}
					}
				}, depth, false);
				if (exception[0] != null) {
					throw exception[0];
				}
			} catch (CoreException e) {
				throw new SVNException(new Status(IStatus.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNTeamProvider.visitError", new Object[] {resources[i].getFullPath()}), e)); //$NON-NLS-1$
			}
		} // for
		// If an exception occured during the visit, throw it here
		if (eHolder[0] != null)
			throw eHolder[0];

		// Add the folders, followed by files!
        SVNClientAdapter svnClient = getSVNWorkspaceRoot().getRepository().getSVNClient();
		progress.beginTask(null, files.size() * 10 + (folders.isEmpty() ? 0 : 10));
        OperationManager.getInstance().beginOperation(svnClient);
		try {
            for(Iterator it=folders.iterator(); it.hasNext();) {
                final ISVNLocalResource localResource = (ISVNLocalResource)it.next();
  
   				try {
					svnClient.addDirectory(localResource.getIResource().getLocation().toFile(),false);
				} catch (ClientException e) {
                    throw SVNException.wrapException(e);
                }
            }

            for(Iterator it=files.iterator(); it.hasNext();) {
                final ISVNLocalResource localResource = (ISVNLocalResource)it.next();
  
                try {
                    svnClient.addFile(localResource.getIResource().getLocation().toFile());
                } catch (ClientException e) {
                    throw SVNException.wrapException(e);
                }    
            }
                

		} finally {
            OperationManager.getInstance().endOperation();
            progress.done();
		}
	}

	/**
	 * Checkin any local changes to given resources
	 * 
	 */
	public void checkin(IResource[] resources, final String comment, final int depth, IProgressMonitor progress) throws TeamException {
		final SVNClientAdapter svnClient = getSVNWorkspaceRoot().getRepository().getSVNClient();
        
        // Prepare the parents list
        // we will Auto-commit parents if they are not already commited
        List parentsList = new ArrayList();
        for (int i=0; i<resources.length; i++) {
            IResource currentResource = resources[i];
            IContainer parent = currentResource.getParent();
            ISVNLocalResource svnParentResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
            while (parent.getType() != IResource.ROOT && 
                   parent.getType() != IResource.PROJECT && 
                   !svnParentResource.hasRemote()) {
                       parentsList.add(parent);
                parent = parent.getParent();
                svnParentResource = svnParentResource.getParent();
            }
        }
        
        // convert parents and resources to an array of File
        final File[] parents = new File[parentsList.size()];
        for (int i = 0; i < parentsList.size();i++)
            parents[i] = ((IResource)parentsList.get(i)).getLocation().toFile();
            
		final File[] resourceFiles = new File[resources.length];
		for (int i = 0; i < resources.length;i++)
			resourceFiles[i] = resources[i].getLocation().toFile(); 
		
        SVNProviderPlugin.run(new ISVNRunnable() {
            public void run(IProgressMonitor monitor) throws SVNException {
                try {
                    monitor.beginTask(null, 100);
                    OperationManager.getInstance().beginOperation(svnClient);
                    
                    // we commit the parents (not recursively)
                    if (parents.length > 0)
                        svnClient.commit(parents,comment,false);
                    
                    // then the resources the user has requested to commit
                    svnClient.commit(resourceFiles,comment,depth == IResource.DEPTH_INFINITE);
                } catch (ClientException e) {
                    throw new SVNException("cannot checkin file");
                } finally {
                    OperationManager.getInstance().endOperation();
                    monitor.done();
                }
            }
        }, Policy.monitorFor(progress));
	}

    /**
     * Update to given revision
     */
    public void update(final IResource[] resources, final Revision revision, IProgressMonitor progress) throws TeamException {
    
        SVNProviderPlugin.run(new ISVNRunnable() {
            public void run(IProgressMonitor monitor) throws SVNException {
                try {
                    monitor.beginTask(null, 100);                    
                    SVNClientAdapter svnClient = getSVNWorkspaceRoot().getRepository().getSVNClient();
                    OperationManager.getInstance().beginOperation(svnClient);
                    for (int i = 0; i < resources.length;i++)
                        svnClient.update(resources[i].getLocation().toFile(),revision,true);
                } catch (ClientException e) {
                    throw new SVNException("cannot update file");
                } finally {
                    OperationManager.getInstance().endOperation();
                    monitor.done();
                }        

            }
        }, Policy.monitorFor(progress));
    }

    /**
     * update to HEAD revision
     */
    public void update(final IResource[] resources, IProgressMonitor progress) throws TeamException {
        update(resources, Revision.HEAD, progress);
    }


    public SVNWorkspaceRoot getSVNWorkspaceRoot() {
        return workspaceRoot;
    }

    public void configureProject() throws CoreException {
        SVNProviderPlugin.broadcastProjectConfigured(getProject());
    }
    /*
     * @see RepositoryProvider#getID()
     */
    public String getID() {
        return SVNProviderPlugin.getTypeId();
    }

    /**
     * Adds a pattern to the set of ignores for the specified folder.
     * 
     * @param folder the folder
     * @param pattern the pattern
     */
    public void addIgnored(ISVNLocalFolder folder, String pattern) throws SVNException {
        if (!folder.getStatus().isManaged())
            throw new SVNException(IStatus.ERROR, SVNException.UNABLE,
                Policy.bind("SVNTeamProvider.ErrorSettingIgnorePattern", folder.getIResource().getFullPath().toString())); //$NON-NLS-1$
        SVNClientAdapter svnClient = getSVNWorkspaceRoot().getRepository().getSVNClient();
        try {
            OperationManager.getInstance().beginOperation(svnClient);
            
            try {
                svnClient.addToIgnoredPatterns(folder.getFile(), pattern);
                
                
                // broadcast changes to unmanaged children - they are the only candidates for being ignored
                ISVNResource[] members = folder.members(null, ISVNFolder.UNMANAGED_MEMBERS);
                IResource[] possiblesIgnores = new IResource[members.length];
                for (int i = 0; i < members.length;i++)
                    possiblesIgnores[i] = ((ISVNLocalResource)members[i]).getIResource(); 
                folder.refreshStatus(IResource.DEPTH_ONE);
                SVNProviderPlugin.broadcastSyncInfoChanges(possiblesIgnores);
            }
            catch (ClientException e) {
                throw SVNException.wrapException(e);
            }

        } finally {
            OperationManager.getInstance().endOperation();
        }
    }
    
	public IMoveDeleteHook getMoveDeleteHook() {
		return new SVNMoveDeleteHook();
	}
    
    

//		
//	/**
//	 * @see ITeamProvider#delete(IResource[], int, IProgressMonitor)
//	 */
//	public void delete(IResource[] resources, final IProgressMonitor progress) throws TeamException {
//		try {
//			progress.beginTask(null, 100);
//			
//			// Delete any files locally and record the names.
//			// Use a resource visitor to ensure the proper depth is obtained
//			final IProgressMonitor subProgress = Policy.infiniteSubMonitorFor(progress, 30);
//			subProgress.beginTask(null, 256);
//			final List files = new ArrayList(resources.length);
//			final TeamException[] eHolder = new TeamException[1];
//			for (int i=0;i<resources.length;i++) {
//				IResource resource = resources[i];
//				checkIsChild(resource);
//				try {
//					if (resource.exists()) {
//						resource.accept(new IResourceVisitor() {
//							public boolean visit(IResource resource) {
//								try {
//									ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
//									if (cvsResource.isManaged()) {
//										String name = resource.getProjectRelativePath().toString();
//										if (resource.getType() == IResource.FILE) {
//											files.add(name);
//											((IFile)resource).delete(false, true, subProgress);
//										}
//									}
//								} catch (TeamException e) {
//									eHolder[0] = e;
//								} catch (CoreException e) {
//									eHolder[0] = wrapException(e);
//									// If there was a problem, don't visit the children
//									return false;
//								}
//								// Always return true and let the depth determine if children are visited
//								return true;
//							}
//						}, IResource.DEPTH_INFINITE, false);
//					} else if (resource.getType() == IResource.FILE) {
//						// If the resource doesn't exist but is a file, queue it for removal
//						files.add(resource.getProjectRelativePath().toString());
//					}
//				} catch (CoreException e) {
//					throw wrapException(e);
//				}
//			}
//			subProgress.done();
//			// If an exception occured during the visit, throw it here
//			if (eHolder[0] != null) throw eHolder[0];		
//			// If there are no files to delete, we are done
//			if (files.isEmpty()) return;
//			
//			// Remove the files remotely
//			IStatus status;
//			Session s = new Session(workspaceRoot.getRemoteLocation(), workspaceRoot.getLocalRoot());
//			s.open(progress);
//			try {
//				status = Command.REMOVE.execute(s,
//				Command.NO_GLOBAL_OPTIONS,
//				Command.NO_LOCAL_OPTIONS,
//				(String[])files.toArray(new String[files.size()]),
//				null,
//				Policy.subMonitorFor(progress, 70));
//			} finally {
//				s.close();
//			}
//			if (status.getCode() == CVSStatus.SERVER_ERROR) {
//				throw new CVSServerException(status);
//			}	
//		} finally {
//			progress.done();
//		}
//	}
//	
//	/** 
//	 * Diff the resources with the repository and write the output to the provided 
//	 * PrintStream in a form that is usable as a patch. The patch is rooted at the
//	 * project.
//	 */
//	public void diff(IResource resource, LocalOption[] options, PrintStream stream,
//		IProgressMonitor progress) throws TeamException {
//		
//		// Determine the command root and arguments arguments list
//		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
//		ICVSFolder commandRoot;
//		String[] arguments;
//		if (cvsResource.isFolder()) {
//			commandRoot = (ICVSFolder)cvsResource;
//			arguments = new String[] {Session.CURRENT_LOCAL_FOLDER};
//		} else {
//			commandRoot = cvsResource.getParent();
//			arguments = new String[] {cvsResource.getName()};
//		}
//
//		Session s = new Session(workspaceRoot.getRemoteLocation(), commandRoot);
//		progress.beginTask(null, 100);
//		try {
//			s.open(Policy.subMonitorFor(progress, 20));
//			Command.DIFF.execute(s,
//				Command.NO_GLOBAL_OPTIONS,
//				options,
//				arguments,
//				new DiffListener(stream),
//				Policy.subMonitorFor(progress, 80));
//		} finally {
//			s.close();
//			progress.done();
//		}
//	}
//	
//	/**
//	 * Replace the local version of the provided resources with the remote using "cvs update -C ..."
//	 * 
//	 * @see ITeamProvider#get(IResource[], int, IProgressMonitor)
//	 */
//	public void get(IResource[] resources, final int depth, IProgressMonitor progress) throws TeamException {
//		get(resources, depth, null, progress);
//	}
//	
//	public void get(final IResource[] resources, final int depth, final CVSTag tag, IProgressMonitor progress) throws TeamException {
//			
//		// Handle the retrival of the base in a special way
//		if (tag != null && tag.equals(CVSTag.BASE)) {
//			new ReplaceWithBaseVisitor().replaceWithBase(getProject(), resources, depth, progress);
//			return;
//		}
//
//		// Make a connection before preparing for the replace to avoid deletion of resources before a failed connection
//		Session.run(workspaceRoot.getRemoteLocation(), workspaceRoot.getLocalRoot(), true /* output to console */,
//			new ICVSRunnable() {
//				public void run(IProgressMonitor progress) throws CVSException {
//					// Prepare for the replace (special handling for "cvs added" and "cvs removed" resources
//					progress.beginTask(null, 100);
//					try {
//						new PrepareForReplaceVisitor().visitResources(getProject(), resources, "CVSTeamProvider.scrubbingResource", depth, Policy.subMonitorFor(progress, 30)); //$NON-NLS-1$
//									
//						// Perform an update, ignoring any local file modifications
//						List options = new ArrayList();
//						options.add(Update.IGNORE_LOCAL_CHANGES);
//						if(depth != IResource.DEPTH_INFINITE) {
//							options.add(Command.DO_NOT_RECURSE);
//						}
//						LocalOption[] commandOptions = (LocalOption[]) options.toArray(new LocalOption[options.size()]);
//						try {
//							update(resources, commandOptions, tag, true /*createBackups*/, Policy.subMonitorFor(progress, 70));
//						} catch (TeamException e) {
//							throw CVSException.wrapException(e);
//						}
//					} finally {
//						progress.done();
//					}
//				}
//			}, progress);
//	}
//	
//	/**
//	 * Return the remote location to which the receiver's project is mapped.
//	 */
//	public ICVSRepositoryLocation getRemoteLocation() throws CVSException {
//		try {
//			return workspaceRoot.getRemoteLocation();
//		} catch (CVSException e) {
//			// If we can't get the remote location, we should disconnect since nothing can be done with the provider
//			try {
//				RepositoryProvider.unmap(project);
//			} catch (TeamException ex) {
//				CVSProviderPlugin.log(ex);
//			}
//			// We need to trigger a decorator refresh					
//			throw e;
//		}
//	}
//	

//	/**
//	 * Update the sync info of the local resource associated with the sync element such that
//	 * the revision of the local resource matches that of the remote resource.
//	 * This will allow commits on the local resource to succeed.
//	 * 
//	 * Only file resources can be merged.
//	 */
//	public void merged(IRemoteSyncElement[] elements) throws TeamException {	
//		for (int i=0;i<elements.length;i++) {
//			((CVSRemoteSyncElement)elements[i]).makeOutgoing(Policy.monitorFor(null));
//		}
//	}
//	
//	/**
//	 * @see ITeamProvider#move(IResource, IPath, IProgressMonitor)
//	 */
//	public void moved(IPath source, IResource resource, IProgressMonitor progress) throws TeamException {
//	}
//
//	/**
//	 * Set the connection method for the given resource's
//	 * project. If the conection method name is invalid (i.e.
//	 * no corresponding registered connection method), false is returned.
//	 */
//	public boolean setConnectionInfo(IResource resource, String methodName, IUserInfo userInfo, IProgressMonitor monitor) throws TeamException {
//		checkIsChild(resource);
//		try {
//			monitor.beginTask(Policy.bind("CVSTeamProvider.connectionInfo", project.getName()), 100); //$NON-NLS-1$
//			
//			if (!CVSRepositoryLocation.validateConnectionMethod(methodName))
//				return false;
//				
//			// Get the original location
//			ICVSRepositoryLocation location = workspaceRoot.getRemoteLocation();
//			
//			// Make a copy to work on
//			CVSRepositoryLocation newLocation = CVSRepositoryLocation.fromString(location.getLocation());
//			newLocation.setMethod(methodName);
//			newLocation.setUserInfo(userInfo);
//	
//			// Validate that a connection can be made with the new location
//			boolean isKnown = CVSProviderPlugin.getPlugin().isKnownRepository(newLocation.getLocation());
//			try {
//				newLocation.validateConnection(Policy.subMonitorFor(monitor, 20));
//			} catch (CVSException e) {
//				if (!isKnown)
//					CVSProviderPlugin.getPlugin().disposeRepository(newLocation);
//				throw e;
//			}
//			
//			// Add the location to the provider
//			CVSProviderPlugin.getPlugin().addRepository(newLocation);
//			
//			// Set the project to use the new Locations
//			setRemoteRoot(newLocation, Policy.subMonitorFor(monitor, 80));
//			return true;
//		} finally {
//			monitor.done();
//		}
//	}
	
	
//	/*
//	 * @see ITeamProvider#refreshState(IResource[], int, IProgressMonitor)
//	 */
//	public void refreshState(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException {
//		Assert.isTrue(false);
//	}
//	
//	/*
//	 * @see ITeamProvider#isDirty(IResource)
//	 */
//	public boolean isDirty(IResource resource) {
//		Assert.isTrue(false);
//		return false;
//	}
//	
//	
//	/*
//	 * Generate an exception if the resource is not a child of the project
//	 */
//	 private void checkIsChild(IResource resource) throws CVSException {
//	 	if (!isChildResource(resource))
//	 		throw new CVSException(new Status(IStatus.ERROR, CVSProviderPlugin.ID, TeamException.UNABLE, 
//	 			Policy.bind("CVSTeamProvider.invalidResource", //$NON-NLS-1$
//	 				new Object[] {resource.getFullPath().toString(), project.getName()}), 
//	 			null));
//	 }
//	 
//	/*
//	 * Get the arguments to be passed to a commit or update
//	 */
//	private String[] getValidArguments(IResource[] resources, LocalOption[] options) throws CVSException {
//		List arguments = new ArrayList(resources.length);
//		for (int i=0;i<resources.length;i++) {
//			checkIsChild(resources[i]);
//			IPath cvsPath = resources[i].getFullPath().removeFirstSegments(1);
//			if (cvsPath.segmentCount() == 0) {
//				arguments.add(Session.CURRENT_LOCAL_FOLDER);
//			} else {
//				arguments.add(cvsPath.toString());
//			}
//		}
//		return (String[])arguments.toArray(new String[arguments.size()]);
//	}
//	
//	private ICVSResource[] getCVSArguments(IResource[] resources) {
//		ICVSResource[] cvsResources = new ICVSResource[resources.length];
//		for (int i = 0; i < cvsResources.length; i++) {
//			cvsResources[i] = CVSWorkspaceRoot.getCVSResourceFor(resources[i]);
//		}
//		return cvsResources;
//	}
//	
//	/*
//	 * This method expects to be passed an InfiniteSubProgressMonitor
//	 */
//	public void setRemoteRoot(ICVSRepositoryLocation location, IProgressMonitor monitor) throws TeamException {
//
//		// Check if there is a differnece between the new and old roots	
//		final String root = location.getLocation();
//		if (root.equals(workspaceRoot.getRemoteLocation())) 
//			return;
//	
//		try {
//			workspaceRoot.getLocalRoot().run(new ICVSRunnable() {
//				public void run(IProgressMonitor progress) throws CVSException {
//					try {
//						// 256 ticks gives us a maximum of 1024 which seems reasonable for folders is a project
//						progress.beginTask(null, 100);
//						final IProgressMonitor monitor = Policy.infiniteSubMonitorFor(progress, 100);
//						monitor.beginTask(Policy.bind("CVSTeamProvider.folderInfo", project.getName()), 256);  //$NON-NLS-1$
//		
//						// Visit all the children folders in order to set the root in the folder sync info
//						workspaceRoot.getLocalRoot().accept(new ICVSResourceVisitor() {
//							public void visitFile(ICVSFile file) throws CVSException {};
//							public void visitFolder(ICVSFolder folder) throws CVSException {
//								monitor.worked(1);
//								FolderSyncInfo info = folder.getFolderSyncInfo();
//								if (info != null) {
//									monitor.subTask(Policy.bind("CVSTeamProvider.updatingFolder", info.getRepository())); //$NON-NLS-1$
//									folder.setFolderSyncInfo(new FolderSyncInfo(info.getRepository(), root, info.getTag(), info.getIsStatic()));
//									folder.acceptChildren(this);
//								}
//							};
//						});
//					} finally {
//						progress.done();
//					}
//				}
//			}, monitor);
//		} finally {
//			monitor.done();
//		}
//	}
//	
//	/*
//	 * Helper to indicate if the resource is a child of the receiver's project
//	 */
//	private boolean isChildResource(IResource resource) {
//		return resource.getProject().getName().equals(project.getName());
//	}
//	
//	private static TeamException wrapException(CoreException e) {
//		return CVSException.wrapException(e);
//	}
//	
//	/*
//	 * @see RepositoryProvider#getMoveDeleteHook()
//	 */
//	public IMoveDeleteHook getMoveDeleteHook() {
//		return moveDeleteHook;
//	}
//	
//	/*
//	 * Return the currently registered Move/Delete Hook
//	 */
//	public static MoveDeleteHook getRegisteredMoveDeleteHook() {
//		return moveDeleteHook;
//	}
//	 
//	/**
//	 * @see org.eclipse.team.core.RepositoryProvider#getFileModificationValidator()
//	 */
//	public IFileModificationValidator getFileModificationValidator() {
//		if (SVNTeamProvider.fileModificationValidator == null) {
//			SVNTeamProvider.fileModificationValidator = SVNTeamProvider.getPluggedInValidator();
//			if (SVNTeamProvider.fileModificationValidator == null) {
//				SVNTeamProvider.fileModificationValidator =super.getFileModificationValidator();
//			}
//		}
//		return SVNTeamProvider.fileModificationValidator;
//	}

//	/**
//	 * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResources()
//	 */
//	public boolean canHandleLinkedResources() {
//		return true;
//	}

//	/**
//	 * @see org.eclipse.team.core.RepositoryProvider#validateCreateLink(org.eclipse.core.resources.IResource, int, org.eclipse.core.runtime.IPath)
//	 */
//	public IStatus validateCreateLink(IResource resource, int updateFlags, IPath location) {
//		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(resource.getParent().getFolder(new Path(resource.getName())));
//		try {
//			if (cvsFolder.isCVSFolder()) {
//				// There is a remote folder that overlaps with the link so disallow
//				return new CVSStatus(IStatus.ERROR, Policy.bind("CVSTeamProvider.overlappingRemoteFolder", resource.getFullPath().toString())); //$NON-NLS-1$
//			} else {
//				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(resource.getParent().getFile(new Path(resource.getName())));
//				if (cvsFile.isManaged()) {
//					// there is an outgoing file deletion that overlaps the link so disallow
//					return new CVSStatus(IStatus.ERROR, Policy.bind("CVSTeamProvider.overlappingFileDeletion", resource.getFullPath().toString())); //$NON-NLS-1$
//				}
//			}
//		} catch (CVSException e) {
//			CVSProviderPlugin.log(e);
//			return e.getStatus();
//		}
//
//		return super.validateCreateLink(resource, updateFlags, location);
//	}
//	

}
