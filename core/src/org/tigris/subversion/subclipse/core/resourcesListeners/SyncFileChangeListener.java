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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

/*
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
	
    public static final String SVN_DIRNAME = ".svn"; //$NON-NLS-1$
    public static final String SVN_ENTRIES = "entries"; //$NON-NLS-1$
	public static final String SVN_DIRPROPS = "dir-props"; //$NON-NLS-1$
	public static final String SVN_PROPS = "props"; //$NON-NLS-1$
				
	
	/*
	 * When a resource changes this method will detect if the changed resources is a meta file that has changed
	 * 
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			final Set changedContainers = new HashSet();
			
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					
					if(resource.getType()==IResource.ROOT) {
						// continue with the delta
						return true;
					}
					
					if (resource.getType() == IResource.PROJECT) {
						// If the project is not accessible, don't process it
						if (!resource.isAccessible()) return false;
					}
															
					String name = resource.getName();
					int kind = delta.getKind();
					
					// if the file has changed but not in a way that we care
					// then ignore the change (e.g. marker changes to files).
					if(kind == IResourceDelta.CHANGED && 
						(delta.getFlags() & INTERESTING_CHANGES) == 0) {
							return true;
					}
					
					IResource[] toBeNotified = new IResource[0];
										
					if(name.equals(SVN_DIRNAME)) {
						handleSVNDir((IContainer)resource, kind);
					}
										
					if(isEntries(resource)) {
						toBeNotified = handleChangedEntries(resource, kind);
					} else
					if(isDirProps(resource)) {
						toBeNotified = handleChangedDirProps(resource, kind);
					} else
					if(isPropFile(resource)) {
						toBeNotified = handleChangedPropFile(resource, kind);
					}
					
                    if(toBeNotified.length>0) {    
						for (int i = 0; i < toBeNotified.length; i++) {
							changedContainers.add(toBeNotified[i]);							
						}
						if(Policy.DEBUG_METAFILE_CHANGES) {
							System.out.println("[svn] metafile changed : " + resource.getFullPath()); //$NON-NLS-1$
						}
						return false; /*don't visit any children we have all the information we need*/
					} else {					
						return true;
					}
				}
			}, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
				
			if(!changedContainers.isEmpty()) {
                for (Iterator it = changedContainers.iterator(); it.hasNext();){
                    IContainer container = (IContainer)it.next();
                    
                    // we update the members. Refresh can be useful in case of revert etc ...
                    container.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
                    ISVNLocalFolder svnContainer = (ISVNLocalFolder)SVNWorkspaceRoot.getSVNResourceFor(container);
                    svnContainer.refreshStatus(IResource.DEPTH_ONE);
                    
                    // the resources that have potentially changed are the members of the folder
                    // and the folder itself
                    IResource[] members = container.members();
                    IResource[] resources = new IResource[members.length+1];
                    resources[0] = container;
                    System.arraycopy(members,0,resources,1,members.length);
                    
                    SVNProviderPlugin.broadcastSyncInfoChanges(resources);
                }
                
			}			
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
				IFile entriesFile = svnDir.getFile(new Path(SVN_ENTRIES));

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
		IContainer parent = resource.getParent();		
		
		if ((parent == null) || 
		    (!parent.getName().equals(SVN_DIRNAME)) || 
		    (!parent.isTeamPrivateMember() && parent.exists()) ) {
			return false;
		}
		
		if (resource.getType() == IResource.FILE &&
            resource.getName().equals(SVN_ENTRIES)) {    
        	return true;
        }
		return false;
	}

	/*
	 * Tells if this resource is a subversion "dir-props" file 
	 */	
	protected boolean isDirProps(IResource resource) {
		IContainer parent = resource.getParent();		
		
		if ((parent == null) || 
			(!parent.getName().equals(SVN_DIRNAME)) || 
			(!parent.isTeamPrivateMember() && parent.exists()) ) {
			return false;
		}
		
		if (resource.getType() == IResource.FILE &&
			resource.getName().equals(SVN_DIRPROPS)) {     
			return true;
		}
		return false;
		
	}

	/*
	 * Tells if this resource is a subversion prop file 
	 */	
	protected boolean isPropFile(IResource resource) {
		// we first verify that parent is props
		IContainer parent = resource.getParent();		
		if ((parent == null)  || 
		    (!parent.getName().equals(SVN_PROPS)) ) {
			return false;
		}
		parent = parent.getParent();
		
		// we then verify that grand-father is svn
		if ((parent == null) || 
			(!parent.getName().equals(SVN_DIRNAME)) || 
			(!parent.isTeamPrivateMember() && parent.exists()) ) {
			return false;
		}
		
		// ok this is a property file
		if (resource.getType() == IResource.FILE) {
			return true;
		}
		return false;
		
	}

	
	protected IContainer[] handleChangedEntries(IResource resource, int kind) {		
		IContainer changedContainer = resource.getParent().getParent();
		if(changedContainer.exists()) {
			return new IContainer[] {changedContainer};
		} else {
			return new IContainer[0];
		}
	}

	protected IContainer[] handleChangedDirProps(IResource resource, int kind) {		
		IContainer changedContainer = resource.getParent().getParent();
		if(changedContainer.exists()) {
			return new IContainer[] {changedContainer};
		} else {
			return new IContainer[0];
		}
	}

	protected IContainer[] handleChangedPropFile(IResource resource, int kind) {		
		IContainer changedContainer = resource.getParent().getParent().getParent();
		if(changedContainer.exists()) {
			return new IContainer[] {changedContainer};
		} else {
			return new IContainer[0];
		}
	}


}
