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


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

import com.qintsoft.jsvn.jni.ClientException;
import com.qintsoft.jsvn.jni.DirEntry;
import com.qintsoft.jsvn.jni.NodeKind;
import com.qintsoft.jsvn.jni.Revision;
import com.qintsoft.jsvn.jni.Status;

/**
 * Represents handles to SVN resource on the local file system. Synchronization
 * information is taken from the .svn subdirectories. 
 * 
 * We implement Comparable so that resources are in the right order (i.e. parents created before children)
 * This is used in SVNTeamProvider.add for ex.
 * 
 * @see LocalFolder
 * @see LocalFile
 */
abstract class LocalResource implements ISVNResource, Comparable {

	protected static final String SEPARATOR = "/"; //$NON-NLS-1$
	protected static final String CURRENT_LOCAL_FOLDER = "."; //$NON-NLS-1$

    static final QualifiedName RESOURCE_SYNC_KEY = new QualifiedName(SVNProviderPlugin.ID, "resource-sync"); //$NON-NLS-1$
		
	/*
	 * The local resource represented by this handle
	 */
	protected IResource resource;
	
	/*
	 * Creates a SVN handle to the provided resource
	 */
	protected LocalResource(IResource resource) {
		Assert.isNotNull(resource);
		this.resource = resource;
	}
	
	/*
	 * @see ISVNResource#exists()
	 */
	public boolean exists() {
		return resource.exists();
	}

	/*
	 * Returns the parent folder of this resource of <code>null</code> if it has no parent
	 * 
	 * @see ISVNLocalResource#getParent()
	 */
	public ISVNLocalFolder getParent() {
		IContainer parent = resource.getParent();
		if (parent==null) {
			return null;
		}
		return new LocalFolder(parent);
	} 

	/*
	 * @see ISVNResource#getName()
	 */
	public String getName() {
		return resource.getName();
	}

	/*
	 * @see ISVNLocalResource#isIgnored()
	 */
	public boolean isIgnored() throws SVNException {
		// a managed resource is never ignored
		if(isManaged() || resource.getType()==IResource.ROOT || resource.getType()==IResource.PROJECT) {
			return false;
		}
		
		// If the resource is a derived or linked resource, it is ignored
		if (resource.isDerived() || resource.isLinked()) {
			return true;
		}
		
		// always ignore .svn
		String name = getName();
		if (name.equals(".svn")) return true; //$NON-NLS-1$
		
		// check the global ignores from Team
		if (Team.isIgnoredHint(resource)) return true;

        // check ignore patterns from the .cvsignore file.
        if (getStatus().isIgnored()) {
            return true;
        }
		
		// check the parent, if the parent is ignored
		// then this resource is ignored also
		ISVNLocalFolder parent = getParent();
		if(parent==null) return false;
		if (parent.isIgnored()) return true;
		
        return false;
	}

    /*
     * @see ISVNLocalResource#setIgnored()
     */
    public void setIgnored() throws SVNException {
        SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject());
        provider.addIgnored(getParent(),resource.getName());
    }
    
	/*
	 * @see ISVNLocalResource#isManaged()
	 */
	public boolean isManaged() throws SVNException {
		return getStatus().isManaged();
	}
    
    public boolean hasRemote() throws SVNException {
        return getStatus().hasRemote();
    }

    /**
     * get the status of the given resource
     */
    public Status getStatus() throws SVNException {
        Status status = null;
        
        try {
            status = (Status) resource.getSessionProperty(RESOURCE_SYNC_KEY);
        } catch (CoreException e) {
            // the resource does not exist
            // we ignore the exception
        }
       
        if (status == null)
        {
            // don't do getRepository().getSVNClient() as we can ask the status of a file
            // that is not associated with a known repository
            // we don't need login & password so this is not a problem   
            SVNClientAdapter svnClient = new SVNClientAdapter();
            try {
				status = svnClient.getStatus(resource.getLocation().toFile());
                resource.setSessionProperty(RESOURCE_SYNC_KEY, status);
            } catch (ClientException e1) {
                throw SVNException.wrapException(e1);
            } catch (CoreException e) {
                // the resource does not exist
                // we ignore the exception
            }
        }
        return status;
    }

	/*
	 * @see Comparable#compareTo(Object)
     * the comparaison is used for example in SVNTeamProvider.add
	 */
	public int compareTo(Object arg0) {
		LocalResource other = (LocalResource)arg0;
        // this way, resources will be in order
		return resource.getFullPath().toString().compareTo(other.resource.getFullPath().toString());
	}

	public IResource getIResource() {
		return resource;
	}

    public File getFile() {
        return resource.getLocation().toFile();
    }

    /**
     * get the workspace root ie the project
     */
	public SVNWorkspaceRoot getWorkspaceRoot() {
		SVNTeamProvider teamProvider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
		return teamProvider.getSVNWorkspaceRoot();
	}

	/**
	 * return the repository that must be used for any operations on this resource
	 */
	public ISVNRepositoryLocation getRepository() throws SVNException {
		return getWorkspaceRoot().getRepository();
	}

    /**
     * get the url of the resource in the repository
     * The resource does not need to exist in repository 
     * @return
     * @throws SVNException
     */
    public URL getUrl() throws SVNException
    {
        
        try {
			return new URL(Util.appendPath(getRepository().getUrl().toString(),resource.getProjectRelativePath().toString()));
		} catch (MalformedURLException e) {
			throw new SVNException("Can't get url for resource "+resource.toString());
		} 
    }

    /**
     * get the remote resource corresponding to the latest revision of this local resource 
     * @return null if there is no remote file corresponding to this local resource
     * @throws SVNException
     */
    public ISVNRemoteResource getLatestRemoteResource() throws SVNException {
        // even if file is not managed, there can be a corresponding resource
        
        // first we get the url of the resource
        URL url = getUrl();
        
        SVNClientAdapter svnClient = getRepository().getSVNClient();
        DirEntry[] dirEntry = null;
        try {
            dirEntry = svnClient.getList(url,Revision.HEAD,false);
        } catch (ClientException e) {
            throw new SVNException("Can't get latest remote resource for "+resource.toString());   
        }
        
        if (dirEntry.length == 0)
            return null; // no remote file
        else
        {
            if (dirEntry[0].getNodeKind() == NodeKind.file)
                return new RemoteFile(
                    null,  // we don't know its parent
                    getRepository(),
                    url,
                    Revision.HEAD,
                    dirEntry[0].getHasProps(),
                    dirEntry[0].getLastChangedRevision(),
                    dirEntry[0].getLastChanged(),
                    dirEntry[0].getLastAuthor()
                );
             else
                return new RemoteFolder(
                    null,  // we don't know its parent
                    getRepository(),
                    url,
                    Revision.HEAD,
                    dirEntry[0].getHasProps(),
                    dirEntry[0].getLastChangedRevision(),
                    dirEntry[0].getLastChanged(),
                    dirEntry[0].getLastAuthor()
                );                
        }
    }

}
