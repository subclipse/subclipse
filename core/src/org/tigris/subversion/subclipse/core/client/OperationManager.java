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
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.util.ReentrantLock;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

import com.qintsoft.jsvn.jni.ClientException;
import com.qintsoft.jsvn.jni.NodeKind;
import com.qintsoft.jsvn.jni.Notify;

/**
 * This class manages jsvn operations.
 * beginOperation must be called before a batch of svn operations
 * and endOperation after
 * 
 * All notifications from jsvn are redirected to console
 * All changed resources are refreshed using resource.refreshLocal
 */
public class OperationManager implements ISVNNotifyListener {

	// track resources that have changed in a given operation
	private ReentrantLock lock = new ReentrantLock();
	
	private Set changedResources = new HashSet();
    private SVNClientAdapter svnClient = null;

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
	public void beginOperation(SVNClientAdapter svnClient) {
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
                        resource.refreshLocal(IResource.DEPTH_ZERO,new NullProgressMonitor());
                    } catch (CoreException e) {
                        throw SVNException.wrapException(e);             
                    }
                }
			}
		} finally {
			lock.release();
		}
	}

	public void setCommand(int command) {
	}
    
	public void setCommandLine(String commandLine) {
	}

    public void setException(ClientException e) {
        
    }

	/* (non-Javadoc)
	 * @see com.qintsoft.jsvn.jni.Notify#onNotify(java.lang.String, int, int, java.lang.String, int, int, long)
	 */
	public void onNotify(
		String path,
		int action,
		int kind,
		String mimeType,
		int contentState,
		int propState,
		long revision) {
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		
        // path is sometimes absolute, sometimes relative.
        // here we make sure it is absolute
        IPath pathEclipse = null;
        try {
            pathEclipse = new Path( (new Path(path)).toFile().getCanonicalPath());
        } catch (IOException e)
        {
            // should never occur ...
            return;
        }

        if (kind == NodeKind.unknown)  { // delete, revert 
            IPath pathEntries = pathEclipse.removeLastSegments(1).append(".svn/entries");
            IResource entries = workspaceRoot.getFileForLocation(pathEntries);
            changedResources.add(entries);
        }
        else
        {
            IResource resource = null;
    		if (kind == NodeKind.dir)		
                resource = workspaceRoot.getContainerForLocation(pathEclipse);
    		else
            if (kind == NodeKind.file)
                resource =  workspaceRoot.getFileForLocation(pathEclipse);
            
            IResource entries = null;
            if (resource != null) 
                entries = resource.getParent().getFile(new Path(".svn/entries")); 
           
            if (resource != null) {
                // this is not really necessary because as .svn/entries is added, the 
                // corresponding directory will be refreshed
                changedResources.add(resource);
            } 
            
            if (entries != null)
                changedResources.add(entries);
        }
	}

}
