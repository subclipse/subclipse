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
 * This class manages jsvn operations.
 * beginOperation must be called before a batch of svn operations
 * and endOperation after
 * 
 * All changed resources are refreshed using resource.refreshLocal
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
                        resource.refreshLocal(IResource.DEPTH_INFINITE,new NullProgressMonitor());
						if(Policy.DEBUG_METAFILE_CHANGES) {
							System.out.println("[svn] file refreshed : " + resource.getFullPath()); //$NON-NLS-1$
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
    		if (kind == SVNNodeKind.DIR)		
                resource = workspaceRoot.getContainerForLocation(pathEclipse);
    		else
            if (kind == SVNNodeKind.FILE)
                resource =  workspaceRoot.getFileForLocation(pathEclipse);
            
            IResource entries = null;
            if (resource != null) 
                entries = resource.getParent().getFolder(new Path(".svn")); 

			// .svn directory will be refreshed so all files in the directory including resource will
			// be refreshed 
           
            if (entries != null)
                changedResources.add(entries);
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
