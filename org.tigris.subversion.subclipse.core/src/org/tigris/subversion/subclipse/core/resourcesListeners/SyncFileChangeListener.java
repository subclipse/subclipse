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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.status.StatusCacheManager;
import org.tigris.subversion.svnclientadapter.SVNConstants;

/**
 * Listens to subversion meta-file changes :
 * - .svn/entries
 * - .svn/dir-props
 * - files in .svn/props
 * When a change occurs in one of these files :
 * - all files in the directory (.svn/..) are refreshed
 * - the svn status of all files in the directory (.svn/..) is refreshed 
 * - a message is sent to all listeners to tell them that all files in the directory (.svn/..) could have
 * changed 
 * 
 * we treat all files in the directory (.svn/..) because we don't want to parse .svn/entries to know 
 * which changes occured
 */
public class SyncFileChangeListener implements IResourceChangeListener {
	
	// consider the following changes types and ignore the others (e.g. marker and description changes are ignored)
	protected int INTERESTING_CHANGES = 	IResourceDelta.CONTENT | 
											IResourceDelta.MOVED_FROM | 
											IResourceDelta.MOVED_TO |
											IResourceDelta.OPEN | 
											IResourceDelta.REPLACED |
											IResourceDelta.TYPE;
	
				
	
	/*
	 * When a resource changes this method will detect if 
	 * the changed resources is a meta file that has changed
	 * 
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			final StatusCacheManager cacheManager = SVNProviderPlugin.getPlugin().getStatusCacheManager();
//			final ChangesCollector changesCollector = new ChangesCollector();
			
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) {
					IResource resource = delta.getResource();	
					if(resource.getType()==IResource.ROOT) {
						// continue with the delta
						return true;
					}
					
					if (resource.getType() == IResource.PROJECT) {
						// If the project is not accessible, don't process it
						if (!resource.isAccessible()) return false;

						// If the Project is not managed by subclipse, don't process it
						if (!SVNWorkspaceRoot.isManagedBySubclipse((IProject)resource))
							return false;
					}
															
					String name = resource.getName();
					int kind = delta.getKind();
					
					//FileModificationManager may already have processed the folder.
					//Since we do not want to refresh the statuses again, we finish the visitor if we already have the statuses
					try {
						if ((resource.getType() == IResource.FOLDER) && (kind == IResourceDelta.ADDED) 
								&& (cacheManager.hasCachedStatus(resource)) && (cacheManager.getStatus(resource).isManaged())) {
							if(Policy.DEBUG_METAFILE_CHANGES) {
								System.out.println("[svn] duplicte ADD change event registered in SyncFileChangeListener: " + resource); //$NON-NLS-1$
							}
							return false;
						}
					} catch (SVNException e) {
						//The get status failed, so just proceed deeper as normal.
						return true;
					}
					
					// if the file has changed but not in a way that we care
					// then ignore the change (e.g. marker changes to files).
					if(kind == IResourceDelta.CHANGED && 
						(delta.getFlags() & INTERESTING_CHANGES) == 0) {
							return true;
					}
					
//					IContainer toBeNotified = null;
										
					if(SVNProviderPlugin.getPlugin().isAdminDirectory(name)) {
						handleSVNDir((IContainer)resource, kind);
					}
										
//					if(isEntries(resource)) {
//						toBeNotified = handleChangedEntries(resource, kind);
//					}
//					
//                    if(toBeNotified != null) {    
//                    	changesCollector.collectChange(toBeNotified);							
//						if(Policy.DEBUG_METAFILE_CHANGES) {
//							System.out.println("[svn] metafile changed : " + resource.getFullPath()); //$NON-NLS-1$
//						}
//						return false; /*don't visit any children we have all the information we need*/
//					} else {					
//						return true;
//					}
					
					return true;
				}
			}, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
				
//			changesCollector.refreshChangedResources();
			
		} catch(CoreException e) {
			SVNProviderPlugin.log(e.getStatus());
        }
	}
	
	/*
	 * If it's a new SVN directory with the canonical child metafiles then mark it as team-private. Otherwise
	 * if changed or deleted
	 */	
	protected void handleSVNDir(IContainer svnDir, int kind) {
		if((kind & IResourceDelta.ALL_WITH_PHANTOMS)!=0) {
			if(kind==IResourceDelta.ADDED) {
				// should this dir be made team-private? If it contains Entries then yes!
				IFile entriesFile = svnDir.getFile(new Path(SVNConstants.SVN_ENTRIES));

				if (entriesFile.exists() &&  !svnDir.isTeamPrivateMember()) {
					try {
						svnDir.setTeamPrivateMember(true);			
						if(Policy.DEBUG_METAFILE_CHANGES) {
							System.out.println("[svn] found a new SVN meta folder, marking as team-private: " + svnDir.getFullPath()); //$NON-NLS-1$
						}
					} catch(CoreException e) {
						SVNProviderPlugin.log(SVNException.wrapException(svnDir, Policy.bind("SyncFileChangeListener.errorSettingTeamPrivateFlag"), e)); //$NON-NLS-1$
					}
				}
			}
		}
	}

	/*
	 * Tells if this resource is a subversion "entries" file 
	 */	
	protected boolean isEntries(IResource resource) {
		if (resource.getType() != IResource.FILE ||
	            !resource.getName().equals(SVNConstants.SVN_ENTRIES)) {    
	        	return false;
	        }

		IContainer parent = resource.getParent();		

		if ((parent != null) && 
		    (SVNProviderPlugin.getPlugin().isAdminDirectory(parent.getName())) && 
		    (parent.isTeamPrivateMember() || !parent.exists()) ) {
			return true;
		}
		
		return false;
	}
	
	protected IContainer handleChangedEntries(IResource resource, int kind) {		
		IContainer changedContainer = resource.getParent();
		IContainer parent           = changedContainer.getParent();
		if((parent != null) && parent.exists()) {
			return changedContainer;
		} else {
			return null;
		}
	}

//	protected final static class ChangesCollector
//	{
//		private Map map = new HashMap();
//		
//		protected void collectChange(IContainer svnFolder)
//		{
//			IProject project = svnFolder.getProject();
//			Set changes = (Set) map.get(project);
//			if (changes == null) {
//				changes = new HashSet();
//				map.put(project, changes);
//			}
//			changes.add(svnFolder);
//		}
//		
//		protected void refreshChangedResources() throws CoreException
//		{
//			for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
//				final Map.Entry element = (Map.Entry) iter.next();
//				SVNProviderPlugin.run(new ISVNRunnable() {
//					public void run(IProgressMonitor monitor) throws SVNException {
//						refreshProjectFolders((Set) element.getValue(), monitor);
//					}}, 
//					(IProject) element.getKey(), null);				
//			}
//		}
//		
//		protected void refreshProjectFolders(Set folders, IProgressMonitor monitor) throws SVNException
//		{
//			boolean initializeListeners = true;
//			for (Iterator it = folders.iterator(); it.hasNext();) {
//				IContainer dotSvnContainer = (IContainer)it.next();
//				IContainer container = dotSvnContainer.getParent();
//
//				// we update the members. Refresh can be useful in case of revert etc ...
//				try {
////					container.refreshLocal(IResource.DEPTH_ONE, Policy.subMonitorFor(monitor, 100, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
//					container.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
//				} catch (CoreException e) {
//					throw SVNException.wrapException(e);
//				}
//				//ISVNLocalFolder svnContainer = (ISVNLocalFolder)SVNWorkspaceRoot.getSVNResourceFor(container);
//				//svnContainer.refreshStatus(IResource.DEPTH_ONE);
//				IResource[] refreshed = SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(container, false);
//
//				SVNProviderPlugin.broadcastSyncInfoChanges(refreshed, initializeListeners);
//				initializeListeners = false;
//			}                			
//		}					
//	}

}
