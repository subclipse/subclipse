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
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Provides a method to get the status of a resource.   
 * It is much more efficient to get the status of a set a resources than only
 * one resource at a time (at least when you use command line interface)
 * So this class asks the status of the given resource but also the status of 
 * near resources for which status needs to be updated.
 */
public class LocalResourceStatusCache {

    static final QualifiedName RESOURCE_SYNC_KEY = new QualifiedName(SVNProviderPlugin.ID, "resource-sync"); //$NON-NLS-1$
    static final int NB_MAX_RESOURCES_STATUS = 20;

    private static void getResourcesSet(Stack stack, Set resourcesSet) {
        ISVNStatus status;
        IResource[] resources;
        IContainer container = (IContainer)stack.pop();
        try {
            resources = container.members();
        } catch (CoreException e) {
            return;
        }
        
        for (int i = 0; i < resources.length;i++) {
            IResource resource = resources[i];
            if (!resourcesSet.contains(resource)) {
                
                try {
                    status = (ISVNStatus) resource.getSessionProperty(RESOURCE_SYNC_KEY);
                    if (status == null) {
                        resourcesSet.add(resource);
                        if (resourcesSet.size() >= NB_MAX_RESOURCES_STATUS) {
                            return;
                        }
                    }
                } catch (CoreException e) {
                    // the resource does not exist
                    // we ignore the exception
                }
            }
            if (resource instanceof Container) {
                stack.push(resource);
            }            
        }
    }

    /**
     * @return an array of IResource near the given resource for which the status 
     * needs to be updated  
     */
    private static IResource[] getResourcesSet(IResource resource) {
        IContainer container = resource.getParent();
        // we use a stack because 
        Stack stack = new Stack();
        stack.push(container);
        Set resourcesSet = new HashSet();
        resourcesSet.add(resource); // make sure we have at least resource in the resource set
        
        while ((resourcesSet.size() < NB_MAX_RESOURCES_STATUS) && (!stack.isEmpty())) {
            getResourcesSet(stack, resourcesSet);
        }
        return (IResource[])resourcesSet.toArray(new IResource[resourcesSet.size()]);
    }

    /**
     * update the status of resource and near resources that also need to be updated
     * @param resource
     * @throws SVNException
     */
    private static void updateStatusSet(IResource resource) throws SVNException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        
        // we get a set of resources for which we will get the status
        IResource[] resources = getResourcesSet(resource);
        File[] files = new File[resources.length];
        for  (int i = 0; i < resources.length;i++) {
            files[i] = resources[i].getLocation().toFile();
        }
     
        // don't do getRepository().getSVNClient() as we can ask the status of a file
        // that is not associated with a known repository
        // we don't need login & password so this is not a problem
        ISVNStatus[] statuses = null;   
        try {
            ISVNClientAdapter svnClientAdapterStatus = SVNProviderPlugin.getPlugin().createSVNClient();
            statuses = svnClientAdapterStatus.getStatus(files);
        } catch (SVNClientException e1) {
            throw SVNException.wrapException(e1);
        }

            
        for (int i = 0; i < statuses.length;i++) {
            ISVNStatus status = statuses[i];
            IPath pathEclipse = null;
            File file = status.getFile();
            try {
                String canonicalPath = file.getCanonicalPath();
                pathEclipse = new Path(canonicalPath);
            } catch (IOException e) {
                // should never occur ...
            }
                
            IResource resourceStatus = null;                
            if (file.isDirectory()) {        
                resourceStatus = workspaceRoot.getContainerForLocation(pathEclipse);
            }
            else 
            if (file.isFile()) {
                resourceStatus =  workspaceRoot.getFileForLocation(pathEclipse);
            }
                
            if (resourceStatus != null) {
                try {
                    resourceStatus.setSessionProperty(RESOURCE_SYNC_KEY, status);
                } catch (CoreException e) {
                    // can't set the property (because the resource does not exist
                    // for example)     
                }
            }
        }
    }

    /**
     * get the status of the given resource
     */
    public static ISVNStatus getStatus(IResource resource) throws SVNException {
        ISVNStatus status = null;
        
        try {
            status = (ISVNStatus) resource.getSessionProperty(RESOURCE_SYNC_KEY);
        } catch (CoreException e) {
            // the resource does not exist
            // we ignore the exception
        }
       
        if (status == null)
        {
            updateStatusSet(resource);
            
            try {
                status = (ISVNStatus) resource.getSessionProperty(RESOURCE_SYNC_KEY);
            } catch (CoreException e) {
                // the resource does not exist
                // we ignore the exception
            }            
        }
        // if status is STILL null then do a bit of ol skool on it because it's prob a deleted file
        if(status == null){
				// don't do getRepository().getSVNClient() as we can ask the status of a file
				// that is not associated with a known repository
				// we don't need login & password so this is not a problem   
				try {
					ISVNClientAdapter svnClientAdapterStatus = SVNProviderPlugin.getPlugin().createSVNClient();
					status = svnClientAdapterStatus.getSingleStatus(resource.getLocation().toFile());
					resource.setSessionProperty(RESOURCE_SYNC_KEY, status);
				} catch (SVNClientException e1) {
					throw SVNException.wrapException(e1);
				} catch (CoreException e) {
					// the resource does not exist
					// we ignore the exception
				}
        	
        }
        return status;
    }

}
