/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
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
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.SVNConstants;

/**
 * This class listens for additions of subversion meta directories and mark them as
 * team private resources. It also registers as a save participant so that deltas
 * generated before the plugin are loaded are not missed. 
 */
public class TeamPrivateListener implements IResourceChangeListener, ISaveParticipant {
	private SVNProviderPlugin provider;

	public TeamPrivateListener()
	{
		super();
		provider = SVNProviderPlugin.getPlugin();
	}
	
	/**
	 * Listen for file modifications
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {		
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					int type = resource.getType();

					if(type==IResource.FOLDER) {
						if (delta.getKind() != IResourceDelta.ADDED)
							return true;
						if (provider.isAdminDirectory(resource.getName())) {
							if (handleSVNDir((IContainer)resource)) {
								return false;
							}
						}
						return true;
					}				
					else if (type==IResource.PROJECT) {
						IProject project = (IProject)resource;
						if (!project.isAccessible()) {
							return false;
						}
						if (!SVNWorkspaceRoot.isManagedBySubclipse(project)) {
							return false; // not a svn handled project
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			SVNProviderPlugin.log(e.getStatus());
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

	/**
	 * If it's a new SVN directory with the canonical child metafiles then mark it as team-private. 
	 * Makr it is team private even when it is changed but not marked team private yet.
	 * @param svnDir IContainer which is expected to be svn meta directory
	 * @param kind resourceDelta kind of change
	 * @return true when the folder folder really is svn meta directory
	 */	
	public boolean handleSVNDir(IContainer svnDir) {
		if (!svnDir.isTeamPrivateMember()) 
		{
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
		return svnDir.isTeamPrivateMember();
	}
}
