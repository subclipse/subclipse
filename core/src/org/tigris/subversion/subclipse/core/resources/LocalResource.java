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
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.commands.AddIgnoredPatternCommand;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNConstants;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

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
public abstract class LocalResource implements ISVNLocalResource, Comparable {
	protected static final String SEPARATOR = "/"; //$NON-NLS-1$
	protected static final String CURRENT_LOCAL_FOLDER = "."; //$NON-NLS-1$

//    static final QualifiedName RESOURCE_SYNC_KEY = new QualifiedName(SVNProviderPlugin.ID, "resource-sync"); //$NON-NLS-1$
		
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
		if (name.equals(SVNConstants.SVN_DIRNAME)) return true; //$NON-NLS-1$
		
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
        AddIgnoredPatternCommand command = new AddIgnoredPatternCommand(getParent(), resource.getName());
        command.run(null);
    }
    
	/*
	 * @see ISVNLocalResource#isManaged()
	 */
	public boolean isManaged() throws SVNException {
		return !this.resource.isDerived() && getStatus().isManaged();
	}
    
    public boolean hasRemote() throws SVNException {
        return !this.resource.isLinked() && getStatus().hasRemote();
    }

    /**
     * get the status of the given resource
     */
    public LocalResourceStatus getStatus() throws SVNException {
    	if (SVNWorkspaceRoot.isLinkedResource(this.resource)) { return LocalResourceStatus.NONE; }
    	LocalResourceStatus aStatus = SVNProviderPlugin.getPlugin().getStatusCacheManager().getStatus(resource);
        return (aStatus != null) ? aStatus : LocalResourceStatus.NONE;
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
	public ISVNRepositoryLocation getRepository()  {
		try {
			return getWorkspaceRoot().getRepository();
		} catch (SVNException e) {
			// an exception is thrown when project is not managed
			SVNProviderPlugin.log(e);
			return null;
		}
	}

    /**
     * get the url of the resource in the repository
     * The resource does not need to exist in repository 
     * @return the url or null if cannot get the url (when project is not managed) 
     * @throws SVNException
     */
    public SVNUrl getUrl()
    {
    	try {
    		if (isManaged()) {
    			// if the resource is managed, get the url directly
    			return getStatus().getUrl();
    		} else {
    			// otherwise, get the url of the parent
    			ISVNLocalResource parent = getParent();
    			if (parent == null) {
    				return null; // we cannot find the url
    			}
   				return parent.getUrl().appendPath(resource.getName());	
    		}
    	} catch (SVNException e) {
    		return null;
    	}
    }

    /**
     * get the remote resource corresponding to the latest revision of this local resource 
     * @return null if there is no remote file corresponding to this local resource
     * @throws SVNException
     */
    public ISVNRemoteResource getLatestRemoteResource() throws SVNException {
        return getRemoteResource(SVNRevision.HEAD); 
    }

    /**
     * get the remote resource corresponding to the given revision of this local resource
     * @return null if there is no remote file corresponding to this local resource
     * @throws SVNException
     */
    public ISVNRemoteResource getRemoteResource(SVNRevision revision) throws SVNException {
        if (revision.equals(SVNRevision.BASE)) {
        	// if the user wants the base resource, we can't get it using the url
        	return getBaseResource();
        }
    	// even if file is not managed, there can be a corresponding resource
        GetRemoteResourceCommand command = new GetRemoteResourceCommand(getRepository(),getUrl(),revision);
        command.run(null);
        return command.getRemoteResource();
    }
    
    /**
     * Remove file or directory from version control.
     */
    public void delete() throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.remove(new File[] { getFile() }, true);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }
    }

    /**
     * Restore pristine working copy file (undo all local edits) 
     */
    public void revert(boolean recurse) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.revert(getFile(), recurse);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }
    }
    
    /**
     * Restore pristine working copy file (undo all local edits) 
     */
    public void resolve() throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.resolved(getFile());
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }
    }
    
	/**
	 * Set a svn property 
	 */
	public void setSvnProperty(String name,String value, boolean recurse) throws SVNException {
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
			OperationManager.getInstance().beginOperation(svnClient);
			svnClient.propertySet(getFile(),name,value,recurse);
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e); 
		} finally {
			OperationManager.getInstance().endOperation();
		}
	}

	/**
	 * Set a svn property 
	 */
	public void setSvnProperty(String name,File value, boolean recurse) throws SVNException {
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
			OperationManager.getInstance().beginOperation(svnClient);
			svnClient.propertySet(getFile(),name,value,recurse);
		} catch (IOException e) {
			throw SVNException.wrapException(e);
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e); 
		} finally {
			OperationManager.getInstance().endOperation();
		}
	}

	/**
	 * Delete a svn property 
	 */
	public void deleteSvnProperty(String name,boolean recurse) throws SVNException {
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
			OperationManager.getInstance().beginOperation(svnClient);
			svnClient.propertyDel(getFile(),name,recurse);
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e); 
		} finally {
			OperationManager.getInstance().endOperation();
		}
	}

	/**
	 * Get a svn property
	 */
	public ISVNProperty getSvnProperty(String name) throws SVNException {
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
			return svnClient.propertyGet(getFile(),name);
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e); 
		}
	}

	/**
	 * Get the svn properties for this resource
	 */
	public ISVNProperty[] getSvnProperties() throws SVNException {
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
			ISVNProperty[] properties = svnClient.getProperties(getFile());
			return properties;
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e); 
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(getIResource())) {
			return getIResource();
		}
		return Platform.getAdapterManager().getAdapter(this,adapter);
	}

}
