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
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProviderType;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.JobUtility;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;

/**
 * This class performs several functions related to determining the modified
 * status of files under Subversion control. First, it listens for change delta's for
 * files and brodcasts them to all listeners. It also registers as a save
 * participant so that deltas generated before the plugin are loaded are not
 * missed. 
 */
public class FileModificationManager implements IResourceChangeListener, ISaveParticipant, IPropertyChangeListener {
	
	private boolean ignoreManagedDerivedResources = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES);
	
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
					
					if (resource.isDerived()) {
						LocalResourceStatus aStatus = null;
						try {
							aStatus = SVNProviderPlugin.getPlugin().getStatusCacheManager().getStatusFromCache(resource);
						} catch (SVNException e) {
							SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
						}
						if ((aStatus == null) || !aStatus.isManaged()) {
							return false;
						}
						if (ignoreManagedDerivedResources) {
							return false;
						}
					}

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
						// FIXME: Why a different processing for add and delete?
						if (delta.getKind() == IResourceDelta.ADDED) {
							if (resource.getParent() != null && !modifiedInfiniteDepthResources.contains(resource.getParent())) {
								modifiedInfiniteDepthResources.add(resource.getParent());
							}
							else {
								modifiedInfiniteDepthResources.add(resource);
							}
							return false;
						}
						else if (delta.getKind() == IResourceDelta.REMOVED) {
							modifiedInfiniteDepthResources.add(resource);
							// No need to add the complete resource tree
							return false;
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
								autoShareProjectIfSVNWorkingCopy(project);
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
			
			if (!modifiedResources.isEmpty() || !modifiedInfiniteDepthResources.isEmpty()) {
				List<IProject> projects = new ArrayList<IProject>();
				if (!modifiedResources.isEmpty()) {
					IResource[] resources = (IResource[])modifiedResources.toArray(new IResource[modifiedResources.size()]);
					for (IResource resource : resources) {
						IProject project = resource.getProject();
						if (project != null && !projects.contains(project)) {
							projects.add(project);
						}
					}
				}
				if (!modifiedInfiniteDepthResources.isEmpty()) {
					IResource[] resources = (IResource[])modifiedInfiniteDepthResources.toArray(new IResource[modifiedInfiniteDepthResources.size()]);
					for (IResource resource : resources) {
						IProject project = resource.getProject();
						if (project != null && !projects.contains(project)) {
							projects.add(project);
						}
					}
				}
				IProject[] projectArray = new IProject[projects.size()];
				projects.toArray(projectArray);
				JobUtility.scheduleJob("Refresh SVN status cache", new Runnable() {				
					public void run() {
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
					}
				}, new RefreshStatusCacheSchedulingRule(MultiRule.combine(projectArray)), false);
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
        	if (resources.length == 1 && resources[0].getType() == IResource.FILE) {
           		try {
                    SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(resource, false);               
        		} catch (SVNException e) {
        			SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
        		}
        	}
        	else {
	            if (resource.getType()==IResource.FILE)
	            {
	                foldersToRefresh.add(resource.getParent());
	            }
	            else
	            {
	                foldersToRefresh.add((IContainer)resource);
	            }
        	}
        }
        for (IResource folder : foldersToRefresh) {
    		try {
                SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus((IContainer)folder, true);               
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

	private void autoShareProjectIfSVNWorkingCopy(IProject project) {
		ISVNClientAdapter client = null;
		try {
			client = SVNProviderPlugin.getPlugin().getSVNClient();
			SVNProviderPlugin.disableConsoleLogging();
			ISVNInfo info = client.getInfoFromWorkingCopy(project.getLocation().toFile());
			if (info != null) {
				SVNTeamProviderType.getAutoShareJob().share(project);
			}
		} catch (Exception e) {}
		finally {
		    SVNProviderPlugin.enableConsoleLogging();
		    if (client != null) {
		    	SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
		    }
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES)) {
			ignoreManagedDerivedResources = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES);
		}
	}
	
	private class RefreshStatusCacheSchedulingRule implements ISchedulingRule {

		public ISchedulingRule schedulingRule;
		
		public RefreshStatusCacheSchedulingRule(ISchedulingRule schedulingRule) {
			this.schedulingRule = schedulingRule;
		}
		
		public boolean contains(ISchedulingRule rule) {
			if (rule instanceof RefreshStatusCacheSchedulingRule) {
				return schedulingRule.contains(((RefreshStatusCacheSchedulingRule)rule).getSchedulingRule());
			}
			return false;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof RefreshStatusCacheSchedulingRule) {
				return schedulingRule.isConflicting(((RefreshStatusCacheSchedulingRule)rule).getSchedulingRule());
			}
			return false;
		}
		
		public ISchedulingRule getSchedulingRule() {
			return schedulingRule;
		}
		
	}

}

