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
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.IMessageHandler;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNClientException;

public class SVNMoveDeleteHook implements IMoveDeleteHook {
	private static Set<IFile> deletedFiles = new HashSet<IFile>();
	
	private void deleteResource(ISVNLocalResource resource) throws SVNException {
		ISVNClientAdapter svnClient = resource.getRepository().getSVNClient();
		 try {
			svnClient.remove(new File[] { resource.getResource().getLocation().toFile() }, true);
		} catch (SVNClientException e) {
			throw new SVNException(IStatus.ERROR, TeamException.UNABLE, e.getMessage(), e);
		}
		finally {
			resource.getRepository().returnSVNClient(svnClient);
		}
	}

    public boolean deleteFile(IResourceTree tree, IFile file, int updateFlags,
            IProgressMonitor monitor) {

        if (SVNWorkspaceRoot.isLinkedResource(file))
            return false;

        ISVNLocalFile resource = new LocalFile(file);
        try {
            if (!resource.isManaged()) {
                return false;
            }

            if (getDeferFileDelete(file))
            	return false;
            
            monitor.beginTask(null, 1000);
            deletedFiles.add(file);
            
            deleteResource(resource);
            
            tree.deletedFile(file);           

        } catch (SVNException e) {
            tree.failed(e.getStatus());
        } finally {
            monitor.done();
        }
        return true;

    }

    public static boolean isDeleted(IFile file) {
    	return deletedFiles.contains(file);
    }
    
    public static void removeFromDeletedFileList(IFile file) {
    	deletedFiles.remove(file);
    }
    
    public boolean deleteFolder(IResourceTree tree, IFolder folder,
            int updateFlags, IProgressMonitor monitor) {

        if (SVNWorkspaceRoot.isLinkedResource(folder))
            return false;

        ISVNLocalFolder resource = new LocalFolder(folder);
        try {

            if (!resource.isManaged()) {
                return false;
            }
            monitor.beginTask(null, 1000);
            deleteResource(resource);
        } catch (SVNException e) {
            tree.failed(e.getStatus());
        } finally {
            monitor.done();
        }
        return true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveFile(org.eclipse.core.resources.team.IResourceTree,
     *      org.eclipse.core.resources.IFile, org.eclipse.core.resources.IFile,
     *      int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean moveFile(IResourceTree tree, IFile source,
            IFile destination, int updateFlags, IProgressMonitor monitor) {

        if (SVNWorkspaceRoot.isLinkedResource(source))
            return false;

        try {
        	
        	RepositoryProvider repositoryProvider = RepositoryProvider
            .getProvider(destination.getProject());
        	
            if (repositoryProvider == null || !(repositoryProvider instanceof SVNTeamProvider)) //target is not SVN project
                throw new SVNException(Policy.bind("SVNMoveHook.moveFileException")); //$NON-NLS-1$
        	
            ISVNLocalFile resource = new LocalFile(source);

            if (!resource.isManaged())
                return false; // pass

            ISVNClientAdapter svnClient = resource.getRepository()
                    .getSVNClient();
            monitor.beginTask(null, 1000);

            try {
                OperationManager.getInstance().beginOperation(svnClient);

                // add destination directory to version control if necessary
                // see bug #15
                if (!SVNWorkspaceRoot.getSVNFolderFor(destination.getParent())
                        .isManaged()) {
                    SVNTeamProvider provider = (SVNTeamProvider) repositoryProvider;
                    provider.add(new IResource[] { destination.getParent() },
                            IResource.DEPTH_ZERO, new NullProgressMonitor());
                    ISVNLocalResource parent = SVNWorkspaceRoot.getSVNResourceFor(destination.getParent());
                    if (parent != null) parent.refreshStatus();
                }

                // force is set to true because when we rename (refactor) a
                // java class, the file is modified before being moved
                // A modified file cannot be moved without force

               svnClient.move(source.getLocation().toFile(), 
            		   destination.getLocation().toFile(), true);

                //movedFile must be done before endOperation because
                // destination file must not already exist in the workspace
                // resource tree.
                tree.movedFile(source, destination);
                destination.refreshLocal(IResource.DEPTH_ZERO, monitor);
            } catch (SVNClientException e) {
                throw SVNException.wrapException(e);
            } catch (TeamException e) {
                throw SVNException.wrapException(e);
            } catch (CoreException e) {
                throw SVNException.wrapException(e);
            } finally {
            	resource.getRepository().returnSVNClient(svnClient);
                OperationManager.getInstance().endOperation(false);
            }

        } catch (SVNException e) {
            tree.failed(e.getStatus());
        } finally {
            monitor.done();
        }
        return true;
    }

    public boolean moveFolder(IResourceTree tree, IFolder source,
            IFolder destination, int updateFlags, IProgressMonitor monitor) {

        if (SVNWorkspaceRoot.isLinkedResource(source))
            return false;

        try {
            ISVNLocalFolder resource = new LocalFolder(source);
            if (!resource.isManaged())
                return false;
            
        	RepositoryProvider repositoryProvider = RepositoryProvider
            .getProvider(destination.getProject());

        	if (repositoryProvider == null || !(repositoryProvider instanceof SVNTeamProvider)) //target is not SVN project
        		 throw new SVNException(Policy.bind("SVNMoveHook.moveFolderException")); //$NON-NLS-1$

            monitor.beginTask(null, 1000);

            ISVNClientAdapter svnClient = resource.getRepository().getSVNClient();

            try {
                OperationManager.getInstance().beginOperation(svnClient);
                // add destination directory to version control if necessary
                // see bug #15
                if (!SVNWorkspaceRoot.getSVNFolderFor(destination.getParent())
                        .isManaged()) {
                    SVNTeamProvider provider = (SVNTeamProvider)repositoryProvider;
                    provider.add(new IResource[] { destination.getParent() },
                            IResource.DEPTH_ZERO, new NullProgressMonitor());
                    ISVNLocalResource parent = SVNWorkspaceRoot.getSVNResourceFor(destination.getParent());
                    if (parent != null) parent.refreshStatus();
               }

               svnClient.move(source.getLocation().toFile(), 
            		   destination.getLocation().toFile(), true);

                tree.movedFolderSubtree(source, destination);
                destination.refreshLocal(IResource.DEPTH_INFINITE, monitor);

            } catch (SVNClientException e) {
                throw SVNException.wrapException(e);

            } catch (CoreException e) {
                throw SVNException.wrapException(e);
            } finally {
            	resource.getRepository().returnSVNClient(svnClient);
                OperationManager.getInstance().endOperation(false);
            }

        } catch (SVNException e) {
            tree.failed(e.getStatus());
        } finally {
            monitor.done();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteProject(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IProject, int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean deleteProject(IResourceTree tree, IProject project, int updateFlags, IProgressMonitor monitor) {
        ISVNLocalFolder resource = new LocalFolder(project);
        try {
        	// If contents are not being deleted, let Eclipse handle.
        	if ((updateFlags & IResource.NEVER_DELETE_PROJECT_CONTENT) == IResource.NEVER_DELETE_PROJECT_CONTENT) {
        		return false;
        	}
        	
        	// If not managed, let Eclipse handle.
			if (!resource.isManaged())
			    return false;
			
			File projectDirectory = new File(project.getLocationURI());
			
			// If meta directory does not exist, let Eclipse handle.
			File metaFolder = new File(projectDirectory, ".svn"); //$NON-NLS-1$
			if (!metaFolder.exists()) {
				return false;
			}
			
			// If database file does not exist, let Eclipse handle.
			File databaseFile = new File(metaFolder, "wc.db"); //$NON-NLS-1$
			if (!databaseFile.exists()) {
				return false;
			}
			
			// If we can delete database file, let Eclipse handle project deletion.
			if (databaseFile.delete()) {
				return false;
			}
			
			// Show message dialog in UI thread and cancel deletion.
			SVNProviderPlugin.handleMessage(Policy.bind("SVNMoveDeleteHook.4"), Policy.bind("SVNMoveDeleteHook.5") + project.getName() + Policy.bind("SVNMoveDeleteHook.6"), IMessageHandler.ERROR); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return true;
			
		} catch (Exception e) {
			// Let Eclipse try to handle it.
			return false;
		}
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveProject(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IProject, org.eclipse.core.resources.IProjectDescription, int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean moveProject(IResourceTree tree, IProject source, IProjectDescription description, int updateFlags, IProgressMonitor monitor) {
        return false;
    }

    // Get the DeferFileDelete Property for selected resource.  First looks at selected resource,
    // then works up through ancestors until a folder with the DeferFileDelete property
    // is found.  If none found, returns false.
    private boolean getDeferFileDelete(IResource resource) {
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        ISVNProperty property = null;
        try {
            if (svnResource.isManaged()) {
                property = svnResource.getSvnProperty("DeferFileDelete"); //$NON-NLS-1$
            }
        } catch (SVNException e) {
        }
        if ((property != null) && (property.getValue() != null) && (property.getValue().trim().length() > 0)) {
            return property.getValue().equalsIgnoreCase("true");            //$NON-NLS-1$
        }
        IResource checkResource = resource;
        while (checkResource.getParent() != null) {
            checkResource = checkResource.getParent();
            if (checkResource.getParent() == null) return false;
            svnResource = SVNWorkspaceRoot.getSVNResourceFor(checkResource);
            try {
                if (svnResource.isManaged())
                    property = svnResource.getSvnProperty("DeferFileDelete"); //$NON-NLS-1$
            } catch (SVNException e1) {
            }
            if ((property != null) && (property.getValue() != null) && (property.getValue().trim().length() > 0)) {
                return property.getValue().equalsIgnoreCase("true");            //$NON-NLS-1$
            }
        }
        return false;
    }

}