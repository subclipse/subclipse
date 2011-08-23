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
package org.tigris.subversion.subclipse.core.resourcesListeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProviderType;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;

/**
 * This class performs several functions related to determining the modified
 * status of files under Subversion control. First, it listens for change delta's for
 * files and brodcasts them to all listeners. It also registers as a save
 * participant so that deltas generated before the plugin are loaded are not
 * missed. 
 */
public class FileModificationManager implements IResourceChangeListener, ISaveParticipant {
	
	// consider the following changes types and ignore the others (e.g. marker and description changes are ignored)
	protected int INTERESTING_CHANGES = IResourceDelta.CONTENT | 
	                                    IResourceDelta.MOVED_FROM | 
										IResourceDelta.MOVED_TO |
										IResourceDelta.OPEN | 
										IResourceDelta.REPLACED |
										IResourceDelta.TYPE;

	/**
	 * Listen for file modifications and fire modification state changes
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			final List<IResource> modifiedResources = new ArrayList<IResource>();
			final List<IResource> modifiedInfiniteDepthResources = new ArrayList<IResource>();

			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) {
					IResource resource = delta.getResource();

					if (resource.getType()==IResource.FILE) {
						if (delta.getKind() == IResourceDelta.CHANGED && resource.exists()) {
							if((delta.getFlags() & INTERESTING_CHANGES) != 0) {
								modifiedResources.add(resource);
								return true;
							}
						} else if (delta.getKind() == IResourceDelta.ADDED) {
							modifiedResources.add(resource);                        
							return true;
						} else if (delta.getKind() == IResourceDelta.REMOVED) {
							// provide notifications for deletions since they may not have been managed
							// The move/delete hook would have updated the parent counts properly
							modifiedResources.add(resource);
							return true;
						}
					}				
					else if(resource.getType()==IResource.FOLDER) {
						if (delta.getKind() == IResourceDelta.ADDED) {
							modifiedInfiniteDepthResources.add(resource);
							return false;
						}
						else if (delta.getKind() == IResourceDelta.REMOVED) {
							modifiedInfiniteDepthResources.add(resource);
							return true;
						}
						return true;
					}				
					else if (resource.getType()==IResource.PROJECT) {
						IProject project = (IProject)resource;
						
						if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
							SVNWorkspaceRoot.unsetManagedBySubclipse(project);
							return false;
						}

						if (!project.isAccessible()) {
							return false;
						}
						if (delta.getKind() != IResourceDelta.ADDED && (delta.getFlags() & IResourceDelta.OPEN) != 0) {
							return false;
						} 
						if (!SVNWorkspaceRoot.isManagedBySubclipse(project)) {
							if (delta.getKind() == IResourceDelta.ADDED) {
								autoShareProjectIfSVNWorkingCopy(resource, project);
							}
							return false; // not a svn handled project
						}
						if (delta.getKind() == IResourceDelta.ADDED) {
							modifiedInfiniteDepthResources.add(resource);
							return false;
						}
						else if (delta.getKind() == IResourceDelta.REMOVED) {
							modifiedInfiniteDepthResources.add(resource);
							return false;
						}
					}
					return true;
				}
			});
            
            // we refresh all changed resources and broadcast the changes to all listeners (ex : SVNLightweightDecorator)
			if (!modifiedResources.isEmpty()) {
                IResource[] resources = (IResource[])modifiedResources.toArray(new IResource[modifiedResources.size()]);
				refreshStatus(resources);
                SVNProviderPlugin.broadcastModificationStateChanges(resources);
			}
			if (!modifiedInfiniteDepthResources.isEmpty()) {
                IResource[] resources = (IResource[])modifiedInfiniteDepthResources.toArray(new IResource[modifiedInfiniteDepthResources.size()]);
                refreshStatusInfitite(resources);
                SVNProviderPlugin.broadcastModificationStateChanges(resources);
			}
		} catch (CoreException e) {
			SVNProviderPlugin.log(e.getStatus());
		}
	}

	/**
	 * Refresh (reset/reload) the status of all the given resources.
	 * @param resources Array of IResources to refresh
     */
    private void refreshStatusInfitite(IResource[] resources) 
    {
    	for (int i = 0; i < resources.length; i++) {
    		try {  			
                SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus((IContainer)resources[i], true);              
                try {
					((IContainer)resources[i]).refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
    		} catch (SVNException e) {
    			SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
    		}			
		}
    }

	/**
	 * Refresh (reset/reload) the status of all the given resources.
	 * @param resources Array of IResources to refresh
     */
    private void refreshStatus(IResource[] resources) {
        //We are not able to get the status for a single file anyway,
        //so from the performance reasons we collect the parent folders of the files
        //and we refresh only those folders then. 
        //All immediate child resources (files) are refreshed automatically
        Set<IContainer> foldersToRefresh = new HashSet<IContainer>(resources.length);
        for (IResource resource : resources) {
            if (resource.getType()==IResource.FILE)
            {
                foldersToRefresh.add(resource.getParent());
            }
            else
            {
                foldersToRefresh.add((IContainer)resource);
            }
        }
        for (IResource folder : foldersToRefresh) {
    		try {
                SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus((IContainer)folder, true);               
                try {
					folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (CoreException e) {}
    		} catch (SVNException e) {
    			SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
    		}
        }
    }
    
    
	/**
	 * We register a save participant so we can get the delta from workbench
	 * startup to plugin startup.
	 * @throws CoreException
	 */
	public void registerSaveParticipant() throws CoreException {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		ISavedState ss = ws.addSaveParticipant(SVNProviderPlugin.getPlugin(), this);
		if (ss != null) {
			ss.processResourceChangeEvents(this);
		}
		ws.removeSaveParticipant(SVNProviderPlugin.getPlugin());
	}
	
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {
	}
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) {
	}
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {
	}
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) {
	}

	private void autoShareProjectIfSVNWorkingCopy(IResource resource,
			IProject project) {
		ISVNClientAdapter client = null;
		try {
			client = SVNProviderPlugin.getPlugin().getSVNClient();
			SVNProviderPlugin.disableConsoleLogging();
			ISVNInfo info = client.getInfoFromWorkingCopy(project.getLocation().toFile());
			if (info != null) {
				SVNTeamProviderType.getAutoShareJob().share((IProject)resource);
			}
		} catch (Exception e) {}
		finally {
		    SVNProviderPlugin.enableConsoleLogging();
		    if (client != null) {
		    	SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
		    }
		}
	}

}

