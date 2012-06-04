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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.ISVNResourceVisitor;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.AddIgnoredPatternCommand;
import org.tigris.subversion.subclipse.core.util.Util;

/**
 * Implements the ISVNLocalFolder interface on top of an instance of the
 * ISVNFolder interface
 * 
 * @see ISVNLocalFolder
 */
public class LocalFolder extends LocalResource implements ISVNLocalFolder {

    /**
     * create a handle based on the given local resource.
     * Container can be IResource.ROOT
     * 
     * @param container
     */
    public LocalFolder(IContainer container) {
        super(container);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#getBaseResource()
     */
    public ISVNRemoteResource getBaseResource() throws SVNException {
        if (!isManaged()) {// no base if no remote
            return null;
        }
        return new BaseFolder(resource, getStatusFromCache());
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNFolder#members(org.eclipse.core.runtime.IProgressMonitor, int)
     */
    public ISVNResource[] members(IProgressMonitor monitor, int flags) throws SVNException {
        if (!resource.exists()) return new ISVNLocalResource[0];
        boolean ignoreHiddenChanges = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES);
        final List<ISVNLocalResource> result = new ArrayList<ISVNLocalResource>();
        IResource[] resources;
        try {
        	boolean isHiddenSupported = true;;
        	if (!ignoreHiddenChanges) {
        		try {
					isHiddenSupported = Util.isHiddenSupported();
				} catch (NoSuchMethodException e) {
					isHiddenSupported = false;
				}        		
        	}
        	if (ignoreHiddenChanges || !isHiddenSupported) {
        		resources = ((IContainer) resource).members(true);
        	}
        	else {
        		// 8 = IContainer.INCLUDE_HIDDEN.
        		resources = ((IContainer) resource).members(8 | IContainer.INCLUDE_PHANTOMS);
        	}
        } catch (CoreException e) {
            throw SVNException.wrapException(e);
        }

        final boolean includeFiles = (((flags & FILE_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		final boolean includeFolders = (((flags & FOLDER_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		final boolean includeManaged = (((flags & MANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS
				| UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		final boolean includeUnmanaged = (((flags & UNMANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS
				| UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		final boolean includeIgnored = ((flags & IGNORED_MEMBERS) != 0);
		final boolean includeExisting = (((flags & EXISTING_MEMBERS) != 0) || ((flags & (EXISTING_MEMBERS | PHANTOM_MEMBERS)) == 0));
		final boolean includePhantoms = (((flags & PHANTOM_MEMBERS) != 0) || ((flags & (EXISTING_MEMBERS | PHANTOM_MEMBERS)) == 0));
        
        for (int i = 0; i < resources.length; i++) {
            if ((includeFiles && (resources[i].getType() == IResource.FILE))
                    || (includeFolders && (resources[i].getType() == IResource.FOLDER))) {
                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
                final boolean isManaged = svnResource.isManaged();
                final boolean isIgnored = svnResource.isIgnored();
                if ((isManaged && includeManaged)
                        || (isIgnored && includeIgnored)
                        || (!isManaged && !isIgnored && includeUnmanaged)) {
                    final boolean exists = svnResource.exists();
                    if ((includeExisting && exists)
                            || (includePhantoms && !exists)) {
                    	if (!ignoreHiddenChanges || !Util.isHidden(resources[i], false)) {
                    		result.add(svnResource);
                    	}
                    }
                }

            }
        }
        return (ISVNLocalResource[]) result
                .toArray(new ISVNLocalResource[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNResource#isFolder()
     */
    public boolean isFolder() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#refreshStatus()
     */
    public void refreshStatus() throws SVNException {
        refreshStatus(false);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFolder#refreshStatus(boolean)
     */
    public void refreshStatus(boolean recursive) throws SVNException {
        SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(
                (IContainer)resource, recursive);
    }

    /**
     * A folder is considered dirty if its status is dirty or if one of its children is dirty
     */
    public boolean isDirty() throws SVNException {
        if (getStatusFromCache().isDirty()) {
            return true;
        }
        // ignored resources are not considered dirty
        ISVNLocalResource[] children = (ISVNLocalResource[]) members(
                new NullProgressMonitor(), ALL_UNIGNORED_MEMBERS);
 
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirty() || children[i].getStatusFromCache().isMissing() || (children[i].exists() && !children[i].isManaged())) {
            	// if a child resource is dirty consider the parent dirty as
                // well, there is no need to continue checking other siblings.
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFolder#acceptChildren(org.tigris.subversion.subclipse.core.ISVNResourceVisitor)
     */
    public void acceptChildren(ISVNResourceVisitor visitor) throws SVNException {

        // Visit files and then folders
        ISVNLocalResource[] subFiles = (ISVNLocalResource[]) members(null,
                FILE_MEMBERS);
        for (int i = 0; i < subFiles.length; i++) {
            subFiles[i].accept(visitor);
        }
        ISVNLocalResource[] subFolders = (ISVNLocalResource[]) members(null,
                FOLDER_MEMBERS);
        for (int i = 0; i < subFolders.length; i++) {
            subFolders[i].accept(visitor);
        }
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#accept(org.tigris.subversion.subclipse.core.ISVNResourceVisitor)
     */
    public void accept(ISVNResourceVisitor visitor) throws SVNException {
        visitor.visitFolder(this);
    }
    
    public IFolder[] getSVNFolders(IProgressMonitor monitor, final boolean unmanage) throws SVNException {
    	final ArrayList<IFolder> svnFolders = new ArrayList<IFolder>();
        SVNProviderPlugin.run(new ISVNRunnable() {
            public void run(IProgressMonitor pm) throws SVNException {
                pm = Policy.monitorFor(pm);
                pm.beginTask(null, 100);

                ISVNResource[] members = members(Policy.subMonitorFor(pm, 20), FOLDER_MEMBERS | MANAGED_MEMBERS);
                ArrayList<IContainer> dirs = new ArrayList<IContainer>();
                for (ISVNResource member : members) {
                    dirs.add((IContainer)((ISVNLocalResource)member).getIResource());
                }
                dirs.add((IContainer)getIResource()); // we add the current folder to the
                // list : we want to add .svn dir
                // for it too

                IProgressMonitor monitorDel = Policy.subMonitorFor(pm, 80);
                monitorDel.beginTask(null, dirs.size());

                for (IContainer container : dirs) {
                    monitorDel.worked(1);
                    recursiveGetSVNFolders(container, monitorDel, unmanage);

                }
                monitorDel.done();
                pm.done();
            }

            private void recursiveGetSVNFolders(IContainer container,
                    IProgressMonitor pm, boolean unmanage) {
                try {
                    // We must not add svn directories for linked resources.
                	if (container.isLinked())
                		return;

                    pm.beginTask(null, 10);
                    pm.subTask(container.getFullPath().toOSString());

                    IResource[] members = container.members(true);
                    for (IResource member : members) {
                        pm.worked(1);
                        if (member.getType() != IResource.FILE) {
                            recursiveGetSVNFolders((IContainer) member, pm, unmanage);
                        }
                    }
                    // Post order traversal
                    IFolder svnFolder = container.getFolder(new Path(
                    		SVNProviderPlugin.getPlugin().getAdminDirectoryName()));
                    if (svnFolder.exists()) {
                        svnFolders.add(svnFolder);
                        if (unmanage) {
                            try {
                                svnFolder.delete(true, null);
                            } catch (CoreException e) {}                        	
                        }
                    }
                } catch (CoreException e) {
                    // Just ignore and continue
                } finally {
                    pm.done();
                }
            }
        }, Policy.subMonitorFor(monitor, 99)); 
    	IFolder[] folderArray = new IFolder[svnFolders.size()];
    	svnFolders.toArray(folderArray);
    	return folderArray;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFolder#unmanage(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void unmanage(IProgressMonitor monitor) throws SVNException {
    	getSVNFolders(monitor, true);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFolder#setIgnoredAs(java.lang.String)
     */
    public void setIgnoredAs(final String pattern) throws SVNException {
        AddIgnoredPatternCommand command = new AddIgnoredPatternCommand(this, pattern);
        command.run(new NullProgressMonitor());
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#revert()
     */
    public void revert() throws SVNException {
        super.revert(true);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#resolve()
     */
    public void resolve() {
    	//Directories could not be resolved.
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#getStatus()
     */
    public LocalResourceStatus getStatus() throws SVNException {
    	if (getIResource().isTeamPrivateMember() && (SVNProviderPlugin.getPlugin().isAdminDirectory(getIResource().getName())))
    	{
    		return LocalResourceStatus.NONE;
    	}
    	if (getIResource() instanceof IWorkspaceRoot)
    	{
    		return LocalResourceStatus.NONE;
    	}
    	return super.getStatus();
    }
    
    public LocalResourceStatus getStatusFromCache() throws SVNException {
    	if (getIResource().isTeamPrivateMember() && (SVNProviderPlugin.getPlugin().isAdminDirectory(getIResource().getName())))
    	{
    		return LocalResourceStatus.NONE;
    	}
    	if (getIResource() instanceof IWorkspaceRoot)
    	{
    		return LocalResourceStatus.NONE;
    	}
    	return super.getStatusFromCache();
    }

}