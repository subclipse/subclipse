/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.util.ReentrantLock;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;



/**
 * This class manages svn operations.
 * beginOperation must be called before a batch of svn operations
 * and endOperation after
 * 
 * All changed .svn directories are refreshed using resource.refreshLocal
 * SyncFileChangeListener will then find that some meta files have changed and will refresh
 * the corresponding resources.
 */
public class OperationManager implements ISVNNotifyListener {

	// track resources that have changed in a given operation
	private ReentrantLock lock = new ReentrantLock();
	
	private Set changedResources = new HashSet();
    private ISVNClientAdapter svnClient = null;

	private static OperationManager instance;
	

	/*
	 * private contructor 
	 */
	private OperationManager() {
		
	}
	
	/**
	 * Returns the singleton instance of the synchronizer.
	 */
	public static OperationManager getInstance() {		
		if(instance==null) {
			instance = new OperationManager();
		}
		return instance;
	}

	/**
	 * Begins a batch of operations.
	 */
	public void beginOperation(ISVNClientAdapter svnClient) {
		lock.acquire();
        this.svnClient = svnClient;
		svnClient.addNotifyListener(this);
		
		if (lock.getNestingCount() == 1) {
			changedResources.clear();
		}		
	}
	
	/**
	 * Ends a batch of operations.  Pending changes are committed only when
	 * the number of calls to endOperation() balances those to beginOperation().
	 */
	public void endOperation() throws SVNException {		
		try {
			if (lock.getNestingCount() == 1) {
                svnClient.removeNotifyListener(this);
                for (Iterator it = changedResources.iterator();it.hasNext(); ) {
                    IResource resource = (IResource)it.next();
                    try {
						// .svn directory will be refreshed so all files in the directory including resource will
						// be refreshed later (@see SyncFileChangeListener) 
                        resource.refreshLocal(IResource.DEPTH_INFINITE,new NullProgressMonitor());
						if(Policy.DEBUG_METAFILE_CHANGES) {
							System.out.println("[svn] .svn dir refreshed : " + resource.getFullPath()); //$NON-NLS-1$
						}
                    } catch (CoreException e) {
                        throw SVNException.wrapException(e);             
                    }
                }
			}
		} finally {
			lock.release();
		}
	}

    public void onNotify(File path, SVNNodeKind kind) {
        		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		
        IPath pathEclipse; 
        try {
            pathEclipse = new Path(path.getCanonicalPath());
        } catch (IOException e)
        {
            // should never occur ...
            return;
        }

        if (kind == SVNNodeKind.UNKNOWN)  { // delete, revert 
            IPath pathEntries = pathEclipse.removeLastSegments(1).append(".svn");
            IResource entries = workspaceRoot.getFolder(pathEntries);
            changedResources.add(entries);
        }
        else
        {
            IResource resource = null;
			IResource svnDir = null;
    		if (kind == SVNNodeKind.DIR) {		
    			// when the resource is a directory, two .svn directories can potentially
    			// be modified
                resource = workspaceRoot.getContainerForLocation(pathEclipse);
				if (resource != null) {
					svnDir = ((IContainer)resource).getFolder(new Path(".svn"));
					changedResources.add(svnDir);
					
					if (resource.getProject() != resource) {
						// if resource is a project. We can't refresh ../.svn						
						svnDir = resource.getParent().getFolder(new Path(".svn"));
						changedResources.add(svnDir); 
					}
				}
    		}
    		else
            if (kind == SVNNodeKind.FILE) {
                resource =  workspaceRoot.getFileForLocation(pathEclipse);
				
				if (resource != null) {
					svnDir = resource.getParent().getFolder(new Path(".svn"));
					changedResources.add(svnDir);
				}
			}            
        }
	}

	public void logCommandLine(String commandLine) {
	}

	public void logCompleted(String message) {
	}

	public void logError(String message) {
	}

	public void logMessage(String message) {
	}

	public void setCommand(int command) {
	}

}
