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
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.repo.ISVNListener;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * provides some static methods to handle repository management 
 * (deletion of remote resources etc ...)
 */
public class RepositoryResourcesManager {
    
    private List<ISVNListener> repositoryListeners = new ArrayList<ISVNListener>();


    /**
     * Register to receive notification of repository creation and disposal
     */
    public void addRepositoryListener(ISVNListener listener) {
        repositoryListeners.add(listener);
    }

    /**
     * De-register a listener
     */
    public void removeRepositoryListener(ISVNListener listener) {
        repositoryListeners.remove(listener);
    }

    /**
     * signals all listener that we have removed a repository 
     */
    public void repositoryRemoved(ISVNRepositoryLocation repository) {
        for (ISVNListener listener : repositoryListeners) {
            listener.repositoryRemoved(repository);
        }    
    }

    /**
     * signals all listener that we have removed a repository 
     */
    public void repositoryAdded(ISVNRepositoryLocation repository) {
    	 for (ISVNListener listener : repositoryListeners) {
            listener.repositoryAdded(repository);
        }    
    }

    /**
     * signals all listener that we have removed a repository 
     */
    public void repositoryModified(ISVNRepositoryLocation repository) {
    	 for (ISVNListener listener : repositoryListeners) {
            listener.repositoryModified(repository);
        }    
    }    
    
    /**
     * signals all listener that a remote resource has been created 
     */
    public void remoteResourceCreated(ISVNRemoteFolder parent, String resourceName) {
    	 for (ISVNListener listener : repositoryListeners) {
            listener.remoteResourceCreated(parent,resourceName);
        }    
    }    

    /**
     * signals all listener that a remote resource has been created 
     */
    public void remoteResourceDeleted(ISVNRemoteResource resource) {
    	for (ISVNListener listener : repositoryListeners) {
            listener.remoteResourceDeleted(resource);
        }    
    } 

    /**
     * signals all listener that a remote resource has been copied 
     */
    public void remoteResourceCopied(ISVNRemoteResource source, ISVNRemoteFolder destination) {
    	for (ISVNListener listener : repositoryListeners) {
            listener.remoteResourceCopied(source, destination);
        }    
    } 

    /**
     * signals all listener that a remote resource has been moved 
     */
    public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder, String destinationResourceName) {
    	for (ISVNListener listener : repositoryListeners) {
            listener.remoteResourceMoved(resource, destinationFolder, destinationResourceName);
        }    
    } 

    
    /**
     * Creates a remote folder 
     */
    public void createRemoteFolder(ISVNRemoteFolder parent, String folderName, String message,IProgressMonitor monitor) throws SVNException {
        parent.createRemoteFolder(folderName, message, monitor);
    }

    /**
     * delete some remote resources
     * Resources can be from several RemoteRepositoryLocations 
     */
    public void deleteRemoteResources(ISVNRemoteResource[] remoteResources, String message,IProgressMonitor monitor) throws SVNException {
        IProgressMonitor progress = Policy.monitorFor(monitor);
        progress.beginTask(Policy.bind("RepositoryResourcesManager.deleteRemoteResources"), 100*remoteResources.length); //$NON-NLS-1$
        
        // the given remote resources can come from more than a repository and so needs
        // more than one svnClient
        // we associate each repository with the corresponding resources to delete
        HashMap<ISVNRepositoryLocation, List<ISVNRemoteResource>> mapRepositories = new HashMap<ISVNRepositoryLocation, List<ISVNRemoteResource>>();
        for (ISVNRemoteResource remoteResource : remoteResources) {
            ISVNRepositoryLocation repositoryLocation = remoteResource.getRepository();
            List<ISVNRemoteResource> resources = (List<ISVNRemoteResource>)mapRepositories.get(repositoryLocation);
            if (resources == null) {
                resources = new ArrayList<ISVNRemoteResource>();
                mapRepositories.put(repositoryLocation, resources);
            }
            resources.add(remoteResource);
        }
        ISVNClientAdapter svnClient = null;
        ISVNRepositoryLocation repository = null;
        try {        
        	for (List<ISVNRemoteResource> resources : mapRepositories.values()) {
                repository = (resources.get(0)).getRepository();
                svnClient = repository.getSVNClient();
                SVNUrl urls[] = new SVNUrl[resources.size()];
                for (int i = 0; i < resources.size();i++) {
                    ISVNRemoteResource resource = resources.get(i); 
                    urls[i] = resource.getUrl();
                    
                    // refresh just says that resource needs to be updated
                    // it does not update immediatly
                    resource.getParent().refresh();
                }
                svnClient.remove(urls,message);
                repository.returnSVNClient(svnClient);
                svnClient = null;
                repository = null;
                
                for (ISVNRemoteResource resource : resources) {
                    remoteResourceDeleted(resource);
                }
                
                progress.worked(100*urls.length);
            }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	if (repository != null) {
        		repository.returnSVNClient(svnClient);
        	}
            progress.done();
        }
    }


    /**
     * copy the remote resource to the given remote folder  
     */
    public void copyRemoteResource(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder, String message,IProgressMonitor monitor) throws SVNException {
        IProgressMonitor progress = Policy.monitorFor(monitor);
        progress.beginTask(Policy.bind("RepositoryResourcesManager.copyRemoteResources"), 100); //$NON-NLS-1$
        ISVNClientAdapter svnClient = null;
        try {        
            svnClient = resource.getRepository().getSVNClient();
            svnClient.copy(resource.getUrl(),destinationFolder.getUrl(),message,SVNRevision.HEAD);
            destinationFolder.refresh();
            remoteResourceCopied(resource, destinationFolder);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	resource.getRepository().returnSVNClient(svnClient);
            progress.done();
        }
    } 

    public void moveRemoteResource(ISVNRemoteResource resource,ISVNRemoteFolder destinationFolder, String destinationResourceName, 
                                   String message,IProgressMonitor monitor) throws SVNException {
        
        IProgressMonitor progress = Policy.monitorFor(monitor);
        progress.beginTask(Policy.bind("RepositoryResourcesManager.moveRemoteResources"), 100); //$NON-NLS-1$
        ISVNClientAdapter svnClient = null;
        try {        
            svnClient = resource.getRepository().getSVNClient();
            SVNUrl destUrl = destinationFolder.getUrl().appendPath(destinationResourceName);
            
            svnClient.move(resource.getUrl(),destUrl,message,SVNRevision.HEAD);
            resource.getParent().refresh();
            destinationFolder.refresh();
            remoteResourceMoved(resource, destinationFolder, destinationResourceName);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	resource.getRepository().returnSVNClient(svnClient);
            progress.done();
        }        
    }


}
