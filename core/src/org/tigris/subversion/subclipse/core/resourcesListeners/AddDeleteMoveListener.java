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
package org.tigris.subversion.subclipse.core.resourcesListeners;

 
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;

/**
 * Listen for the addition or deletion of resource and create appropriate markers
 */
public class AddDeleteMoveListener implements IResourceDeltaVisitor, IResourceChangeListener, IResourceStateChangeListener {

	public static final String SVN_MARKER = "org.tigris.subversion.subclipse.core.svnmarker";//$NON-NLS-1$
	public static final String DELETION_MARKER = "org.tigris.subversion.subclipse.core.svnremove";//$NON-NLS-1$
	
	public static final String NAME_ATTRIBUTE = "name";//$NON-NLS-1$
	

	/**
	 * @see IResourceDeltaVisitor#visit(IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		IProject project = resource.getProject();
		boolean movedTo = (delta.getFlags() & IResourceDelta.MOVED_TO) > 0;
		boolean movedFrom = (delta.getFlags() & IResourceDelta.MOVED_FROM) > 0;
        
        // we refresh the status of the resource
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        
		svnResource.refreshStatus();
        
		switch (delta.getKind()) {
			case IResourceDelta.ADDED :
				// make sure the added resource isn't a phantom
				if (resource.exists()) {
                    createNecessaryMarkers(new IResource[] {resource});
				}	
				break;
			case IResourceDelta.REMOVED :
                createNecessaryMarkers(new IResource[] {resource});
				break;
			case IResourceDelta.CHANGED :
				// This state means there is a resource before and after but changes were made by deleting and moving.
				// For files, we shouldn'd do anything.
				// For folders, we should purge the SVN  info ?
				if (resource.getType() == IResource.FOLDER && resource.exists()) {
					if ((delta.getFlags() & IResourceDelta.REPLACED) > 0) {
                        // The resource has been replaced by a different resource at the same location
                        createNecessaryMarkers(new IResource[] {resource});
						return true;
					}
				}
				break;
		}
		return true;
	}
	
    /**
     * The main method. This method is called when a resource is changed
     */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			IResourceDelta root = event.getDelta();
			IResourceDelta[] projectDeltas = root.getAffectedChildren();
			for (int i = 0; i < projectDeltas.length; i++) {							
				final IResourceDelta delta = projectDeltas[i];
				IResource resource = delta.getResource();
				
				if (resource.getType() == IResource.PROJECT) {
					// If the project is not accessible, don't process it
					if (!resource.isAccessible()) continue;
					if ((delta.getFlags() & IResourceDelta.OPEN) != 0) continue;
				}
				
                // get the SVN provider for the given project or null if not a svn project 
				RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());	

/*				// if a project is moved the originating project will not be associated with the SVN provider
				// however listeners will probably still be interested in the move delta.	
				if ((delta.getFlags() & IResourceDelta.MOVED_TO) > 0) {																
					IResource destination = getResourceFor(resource.getProject(), resource, delta.getMovedToPath());
					provider = RepositoryProvider.getProvider(destination.getProject());
				}
*/				
				if(provider!=null) {
					// Traverse the delta is a runnable so that files are only written at the end
					SVNProviderPlugin.run(new ISVNRunnable() {
						public void run(IProgressMonitor monitor) throws SVNException {
							try {
								delta.accept(AddDeleteMoveListener.this);
							} catch (CoreException e) {
								Util.logError(Policy.bind("ResourceDeltaVisitor.visitError"), e);//$NON-NLS-1$
							}
						}
					}, Policy.monitorFor(null));
				}
			}
		} catch (SVNException e) {
			Util.logError(Policy.bind("ResourceDeltaVisitor.visitError"), e);//$NON-NLS-1$
		}
	}
	
	/*
	 * @see IResourceStateChangeListener#resourceStateChanged(IResource[])
	 */
	public void resourceSyncInfoChanged(IResource[] changedResources) {
		createNecessaryMarkers(changedResources);
	}
			
	/**
	 * @see IResourceStateChangeListener#projectConfigured(IProject)
	 */
	public void projectConfigured(final IProject project) {
		try {
			refreshMarkers(project);
		} catch (CoreException e) {
            SVNProviderPlugin.log(e.getStatus());            
		}
	}

	/**
	 * @see IResourceStateChangeListener#projectDeconfigured(IProject)
	 */
	public void projectDeconfigured(IProject project) {
		try {
			clearSVNMarkers(project);
		} catch (CoreException e) {
			SVNProviderPlugin.log(e.getStatus());
		}
	}
	
    /**
     * refresh all the markers for all the projects 
     */
	public static void refreshAllMarkers() throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if(RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId()) != null) {
				refreshMarkers(project);
			}
		}		
	}
	
    /**
     * create a delete marker for the given resource. marker is stored on its parent
     */
	private static IMarker createDeleteMarker(IResource resource) {
		try {
			IMarker marker = getDeletionMarker(resource);
			if (marker != null) {
				return marker;
			}
			IContainer parent = resource.getParent();
			if (! parent.exists()) return null;
			marker = parent.createMarker(DELETION_MARKER);
			marker.setAttribute("name", resource.getName());//$NON-NLS-1$
			marker.setAttribute(IMarker.MESSAGE, Policy.bind("AddDeleteMoveListener.deletedResource", resource.getName()));//$NON-NLS-1$
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			return marker;
		} catch (CoreException e) {
			Util.logError(Policy.bind("AddDeleteMoveListener.Error_creating_deletion_marker_1"), e); //$NON-NLS-1$
		}
		return null;
	}
	
    /**
     * get a delete marker for the given resource. marker is stored on its parent folder 
     */
	private static IMarker getDeletionMarker(IResource resource) throws CoreException {
		if (resource.getParent().exists()) {
			String name = resource.getName();
	   		IMarker[] markers = resource.getParent().findMarkers(DELETION_MARKER, false, IResource.DEPTH_ZERO);
	   		for (int i = 0; i < markers.length; i++) {
				IMarker iMarker = markers[i];
				String markerName = (String)iMarker.getAttribute(NAME_ATTRIBUTE);
				if (markerName.equals(name))
					return iMarker;
			}
		}
		return null;
	}
	
    /**
     * create the necessary markers for given changedResources (either added or deleted) 
     */
	private static void createNecessaryMarkers(IResource[] changedResources) {
		for (int i = 0; i < changedResources.length; i++) {
			try {
				final IResource resource = changedResources[i];
				
				if (resource.exists()) {
                    // first case : added resource
                    
					IMarker marker;
					// Delete any deletion markers stored on the parent
					if (resource.getType() == IResource.FILE) {
						marker = getDeletionMarker(resource);
						if (marker != null)
							marker.delete();
					}
				} else if (resource.getType() == IResource.FILE) {
                    // second case : deleted resource 
                    
					// Handle deletion markers on non-existant files
					RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());	
					if (provider == null) break;
					ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
					if (svnResource.isManaged()) {
						createDeleteMarker(resource);
					} else {
						IMarker marker = getDeletionMarker(resource);
						if (marker != null)
							marker.delete();
					}
				}
			} catch (SVNException e) {
				Util.logError(Policy.bind("AddDeleteMoveListener.Error_updating_marker_state_4"), e); //$NON-NLS-1$
			} catch (CoreException e) {
				Util.logError(Policy.bind("AddDeleteMoveListener.Error_updating_marker_state_4"), e); //$NON-NLS-1$
			}
		}
	}
	
    /**
     * refresh the markers for the given resource 
     */
	private static void refreshMarkers(IResource resource) throws CoreException {
		final List resources = new ArrayList();
		clearSVNMarkers(resource);
		resource.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if(resource.getType() != IResource.PROJECT) { 
					resources.add(resource);
				}
				return true;
			}
		}, IResource.DEPTH_INFINITE, true /*include phantoms*/);
		createNecessaryMarkers((IResource[]) resources.toArray(new IResource[resources.size()]));
	}
	
    /**
     * clear all svn markers for all projects 
     */
	public static void clearAllSVNMarkers() throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if(RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId()) != null) {
				clearSVNMarkers(project);
			}
		}
	}
	
    /**
     * clear all SVN markers for the given resource 
     */
	private static void clearSVNMarkers(IResource resource) throws CoreException {
		IMarker[] markers = resource.findMarkers(SVN_MARKER, true, IResource.DEPTH_INFINITE);
		for (int i = 0; i < markers.length; i++) {
			markers[i].delete();
		}
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceModificationStateChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceModified(IResource[] changedResources) {
		// Nothing to do here
	}
}
