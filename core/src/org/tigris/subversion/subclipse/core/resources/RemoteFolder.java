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


import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.DirEntry;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * This class provides the implementation of ISVNRemoteFolder
 * 
 */
public class RemoteFolder extends RemoteResource implements ISVNRemoteFolder, ISVNFolder {

    private ISVNRemoteResource[] children;
	
	/**
	 * Constructor for RemoteFolder.
	 */
	public RemoteFolder(RemoteFolder parent, 
        ISVNRepositoryLocation repository,
        SVNUrl url,
        Revision revision,
        boolean hasProps,
        Revision.Number lastChangedRevision,
        Date date,
        String author) {
		super(parent, repository, url, revision,hasProps, lastChangedRevision, date, author);
	}

    public RemoteFolder(ISVNRepositoryLocation repository, SVNUrl url, Revision revision) {
        super(repository, url,revision);
    }
	
	/*
	 * @see ISVNRemoteResource#exists(IProgressMonitor)
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException {
		try {
			getMembers(monitor);
			return true;
		} catch (SVNException e) {
			if (e.getStatus().getCode() == SVNStatus.DOES_NOT_EXIST) {
				return false;
			} else {
				throw e;
			}
		}
	}

	/*
	 * Check whether the given child exists 
	 */
	protected boolean exists(final ISVNRemoteResource child, IProgressMonitor monitor) throws SVNException {
        ISVNRemoteResource[] children;
        try {
            children = getMembers(monitor);
        } catch (SVNException e) {
            if (e.getStatus().getCode() == SVNStatus.DOES_NOT_EXIST) {
                return false;
            } else {
                throw e;
            }
        }
        
        for (int i = 0; i < children.length;i++) {
            if (children[i].equals(child))
                return true;
        }
        return false;
	}

    /**
     * empty cache of children
     */
    public void refresh() {
        children = null;
    }

    /**
     * get the members of this folder 
     */
	protected ISVNRemoteResource[] getMembers(IProgressMonitor monitor) throws SVNException {

		final IProgressMonitor progress = Policy.monitorFor(monitor);
		progress.beginTask(Policy.bind("RemoteFolder.getMembers"), 100); //$NON-NLS-1$
        
        if (children != null)
        {
            progress.done();
            return children;
        }
		
		try {
            SVNClientAdapter client = getRepository().getSVNClient();
				
			DirEntry[] list = client.getList(url,getRevision(),false);
			List result = new ArrayList();

			// directories first				
			for (int i=0;i<list.length;i++)
			{
                DirEntry entry = list[i];
                if (entry.getNodeKind() == NodeKind.dir)
				{
				    result.add(new RemoteFolder(this, getRepository(),
					   new SVNUrl(Util.appendPath(url.toString(),entry.getPath())),
                       getRevision(),
                       entry.getHasProps(),
                       entry.getLastChangedRevision(),
                       entry.getLastChanged(),
                       entry.getLastAuthor()));
				}
			}

			// files then				
			for (int i=0;i<list.length;i++)
			{
				DirEntry entry = list[i];
				if (entry.getNodeKind() == NodeKind.file)
				{
					result.add(new RemoteFile(this, getRepository(),
                        new SVNUrl(Util.appendPath(url.toString(),entry.getPath())),
                        getRevision(),
                        entry.getHasProps(),
                        entry.getLastChangedRevision(),
                        entry.getLastChanged(),
                    entry.getLastAuthor()));
			     }
					 	
			}

			children = (ISVNRemoteResource[])result.toArray(new ISVNRemoteResource[0]);
            return children;
        } catch (ClientException e)
		{
            throw new SVNException(new SVNStatus(SVNStatus.ERROR, SVNStatus.DOES_NOT_EXIST, Policy.bind("RemoteFolder.doesNotExist", getRepositoryRelativePath()))); //$NON-NLS-1$
		} catch (MalformedURLException e)
		{
            throw new SVNException(new SVNStatus(SVNStatus.ERROR, SVNStatus.DOES_NOT_EXIST, Policy.bind("RemoteFolder.doesNotExist", getRepositoryRelativePath()))); //$NON-NLS-1$
        } finally {
			progress.done();
		}	 	
	}


    /**
	 * @see ISVNFolder#members(int)
	 */
	public ISVNResource[] members(IProgressMonitor monitor,int flags) throws SVNException {		
		final List result = new ArrayList();
		ISVNRemoteResource[] resources = getMembers(monitor);

		// RemoteFolders never have phantom members
		if ((flags & EXISTING_MEMBERS) == 0 && (flags & PHANTOM_MEMBERS) == 1) {
			return new ISVNResource[0];
		}
		boolean includeFiles = (((flags & FILE_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeFolders = (((flags & FOLDER_MEMBERS) != 0) || ((flags & (FILE_MEMBERS | FOLDER_MEMBERS)) == 0));
		boolean includeManaged = (((flags & MANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		boolean includeUnmanaged = (((flags & UNMANAGED_MEMBERS) != 0) || ((flags & (MANAGED_MEMBERS | UNMANAGED_MEMBERS | IGNORED_MEMBERS)) == 0));
		for (int i = 0; i < resources.length; i++) {
			ISVNResource svnResource = resources[i];
			if ((includeFiles && ( ! svnResource.isFolder())) 
					|| (includeFolders && (svnResource.isFolder()))) {
				if (includeManaged) {
					result.add(svnResource);
				}
						
			}		
		}
		return (ISVNResource[]) result.toArray(new ISVNResource[result.size()]);
	}
	
	/**
	 * @see ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return true;
	}

	/*
	 * @see IRemoteResource#isContainer()
	 */
	public boolean isContainer() {
		return true;
	}
	
	/*
	 * @see IRemoteResource#members(IProgressMonitor)
	 */
	public IRemoteResource[] members(IProgressMonitor progress) throws TeamException {
		return getMembers(progress);
	}

	/*
	 * @see IRemoteResource#getContents(IProgressMonitor)
	 */
	public InputStream getContents(IProgressMonitor progress) throws TeamException {
		return null;
	}
    
    /**
     * creates a new remote folder 
     */
    public void createRemoteFolder(String folderName, String message,IProgressMonitor monitor) throws SVNException {
        IProgressMonitor progress = Policy.monitorFor(monitor);
        progress.beginTask(Policy.bind("RemoteFolder.createRemoteFolder"), 100); //$NON-NLS-1$
        
        try {
            SVNClientAdapter svnClient = getRepository().getSVNClient();
            svnClient.mkdir(new SVNUrl(Util.appendPath(getUrl().toString(),folderName)),message);
            refresh();
            SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().remoteResourceCreated(this,folderName);
        } catch (MalformedURLException e) {
            throw SVNException.wrapException(e);
        } catch (ClientException e) {
            throw SVNException.wrapException(e);
        } finally {
            progress.done();
        }
    }    
    
}
