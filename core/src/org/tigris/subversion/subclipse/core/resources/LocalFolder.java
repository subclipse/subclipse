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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.ISVNResourceVisitor;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Implements the ISVNLocalFolder interface on top of an 
 * instance of the ISVNFolder interface
 * 
 * @see ISVNLocalFolder
 */
public class LocalFolder extends LocalResource implements ISVNLocalFolder {

    /**
     * create a handle based on the given local resource
     * container can be IResource.ROOT
     *   
     * @param container
     */
	public LocalFolder(IContainer container) {
		super(container);		
	}

    /**
     * @return the remote resource
     */
	public ISVNRemoteResource getRemoteResource() throws SVNException {
		if (!isManaged())
			return null;
		Status status = getStatus();
        SVNUrl url;
        try {
            url = new SVNUrl(status.getUrl());
        } catch (MalformedURLException e) {
            throw new SVNException(e.getMessage());
        }
		return new RemoteFolder(
			null, // parent : we don't know it 
			getRepository(),
            url, 
            Revision.BASE,
			false, // hasProps
			status.getLastChangedRevision(),
			status.getLastChangedDate(),
			status.getLastCommitAuthor());
	}
	
	/**
	 * @see ISVNFolder#members(IProgressMonitor,int)
	 */
	public ISVNResource[] members(IProgressMonitor monitor,int flags) throws SVNException {		
		final List result = new ArrayList();
        IResource[] resources;
        try {
            resources = ((IContainer)resource).members(true);
        } catch (CoreException e) {
            throw SVNException.wrapException(e);
        }           

		boolean includeFiles = (((flags & FILE_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeFolders = (((flags & FOLDER_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeManaged = (((flags & MANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeUnmanaged = (((flags & UNMANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeIgnored = ((flags & IGNORED_MEMBERS) != 0);
		boolean includeExisting = (((flags & EXISTING_MEMBERS) != 0) || ((flags & (EXISTING_MEMBERS | PHANTOM_MEMBERS)) == 0));
		boolean includePhantoms = (((flags & PHANTOM_MEMBERS) != 0) || ((flags & (EXISTING_MEMBERS | PHANTOM_MEMBERS)) == 0));
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if ((includeFiles && (resource.getType()==IResource.FILE)) 
					|| (includeFolders && (resource.getType()==IResource.FOLDER))) {
				boolean isManaged = svnResource.isManaged();
				boolean isIgnored = svnResource.isIgnored();
				if ((isManaged && includeManaged)|| (isIgnored && includeIgnored)
						|| ( ! isManaged && ! isIgnored && includeUnmanaged)) {
					boolean exists = svnResource.exists();
					if ((includeExisting && exists) || (includePhantoms && !exists)) {
						result.add(svnResource);
					}
				}
						
			}		
		}	
		return (ISVNLocalResource[]) result.toArray(new ISVNLocalResource[result.size()]);
	}

	
	/**
	 * @see ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return true;
	}

    /**
     * @see ISVNLocalResource#refreshStatus()
     */
    public void refreshStatus() {
        refreshStatus(IResource.DEPTH_ZERO);    
    }

    /**
     * @see ISVNLocalFolder#refreshStatus(int)
     */
    public void refreshStatus(int depth) {
        try {
            resource.setSessionProperty(RESOURCE_SYNC_KEY, null);
        
            if (depth != IResource.DEPTH_ZERO)
            {
                IResource[] children = ((IContainer)resource).members(); 
                for (int i = 0;i < children.length;i++) {
                    ISVNLocalResource svnChild = SVNWorkspaceRoot.getSVNResourceFor(children[i]);
                    
                    if (svnChild.isFolder()) {
                        ISVNLocalFolder svnFolder = (ISVNLocalFolder)svnChild;
                        if (depth == IResource.DEPTH_ONE)
                            svnFolder.refreshStatus(IResource.DEPTH_ZERO);
                        else
                        if (depth == IResource.DEPTH_INFINITE)
                            svnFolder.refreshStatus(IResource.DEPTH_INFINITE);
                    }
                    else 
                        svnChild.refreshStatus();
                                        
                }
            }        
        
        } catch (CoreException e) {
            // the resource does not exist, we ignore the exception
        }
        
    }
    
	/**
	 * Assumption this is only called from decorator and isIgnored() is purposely
	 * ommited here for performance reasons. 
	 */
	public boolean isModified() throws SVNException {
        IContainer container = (IContainer)getIResource();
          
        if (!isManaged()) {
            return container.exists();
        }
        
        ISVNLocalResource[] children = (ISVNLocalResource[])members(new NullProgressMonitor(),ALL_UNIGNORED_MEMBERS);

        for (int i = 0; i < children.length; i++) {
            ISVNLocalResource resource = children[i];
            if (resource.isModified()) {
                // if a child resource is dirty consider the parent dirty as well, there
                // is no need to continue checking other siblings.
                return true;
          }
      }
      return false;        
	}

    /**
     * @see ISVNFolder#acceptChildren(ICVSResourceVisitor)
     */
    public void acceptChildren(ISVNResourceVisitor visitor) throws SVNException {
        
        // Visit files and then folders
        ISVNLocalResource[] subFiles = (ISVNLocalResource[])members(null,FILE_MEMBERS);
        for (int i=0; i<subFiles.length; i++) {
            subFiles[i].accept(visitor);
        }
        ISVNLocalResource[] subFolders = (ISVNLocalResource[])members(null,FOLDER_MEMBERS);
        for (int i=0; i<subFolders.length; i++) {
            subFolders[i].accept(visitor);
        }
    }

    /**
     * @see ISVNResource#accept(ISVNResourceVisitor)
     */
    public void accept(ISVNResourceVisitor visitor) throws SVNException {
        visitor.visitFolder(this);
    }

    /**
     * unmanage the folder, ie delete its svn subdirectory.
     * Unmanage all its subdirectories too
     */
    public void unmanage(IProgressMonitor monitor) throws SVNException {
        SVNProviderPlugin.run(new ISVNRunnable() {
            public void run(IProgressMonitor monitor) throws SVNException {
                monitor = Policy.monitorFor(monitor);
                monitor.beginTask(null, 100);

            
                ISVNResource[] members = members(Policy.subMonitorFor(monitor, 20),FOLDER_MEMBERS | MANAGED_MEMBERS);
                ArrayList dirs = new ArrayList();
                for (int i = 0; i < members.length;i++)
                    dirs.add(((ISVNLocalResource)members[i]).getIResource());
                dirs.add(getIResource()); // we add the current folder to the list : we want to delete .svn dir for it too 
                
                IProgressMonitor monitorDel = Policy.subMonitorFor(monitor,80);
                monitorDel.beginTask(null,dirs.size());
                 
                
                for (int i = 0; i < dirs.size();i++) {
                    monitorDel.worked(1);
                    IContainer container = (IContainer)dirs.get(i);
                    IFolder svnFolder = container.getFolder(new Path(".svn"));
                    if (svnFolder.exists()) {
                        try {
							svnFolder.delete(true,null);
						} catch (CoreException e) {
						}
                    }                    
                }
                monitorDel.done();
                monitor.done(); 
            }
        }, Policy.subMonitorFor(monitor, 99));
    }


    /*
     * @see ISVNLocalFolder#setIgnoredAs(String)
     */
    public void setIgnoredAs(final String pattern) throws SVNException {
        SVNProviderPlugin.run(new ISVNRunnable() {
            public void run(IProgressMonitor monitor) throws SVNException {
                SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject());
                provider.addIgnored(LocalFolder.this, pattern);
            }
        }, null);
    }    
    
    
}
