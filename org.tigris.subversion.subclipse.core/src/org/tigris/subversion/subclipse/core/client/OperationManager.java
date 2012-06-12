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
package org.tigris.subversion.subclipse.core.client;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.FilteringContainerList;
import org.tigris.subversion.subclipse.core.util.ReentrantLock;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * This class manages svn operations. beginOperation must be called before a
 * batch of svn operations and endOperation after
 * 
 * All changed .svn directories are refreshed using resource.refreshLocal
 * SyncFileChangeListener will then find that some meta files have changed and
 * will refresh the corresponding resources.
 */
public class OperationManager implements ISVNNotifyListener {
	// track resources that have changed in a given operation
	private ReentrantLock lock = new ReentrantLock();

//	private Set changedResources = new LinkedHashSet();
	
	private Set<IResource> localRefreshList = new LinkedHashSet<IResource>();

	private ISVNClientAdapter svnClient = null;

	private OperationProgressNotifyListener operationNotifyListener = null;
	
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
		if (instance == null) {
			instance = new OperationManager();
		}
		return instance;
	}

	/**
	 * Begins a batch of operations.
	 */
	public void beginOperation(ISVNClientAdapter aSvnClient) {
		lock.acquire();
		this.svnClient = aSvnClient;
		aSvnClient.addNotifyListener(this);
		if (operationNotifyListener != null) {
			aSvnClient.setProgressListener(operationNotifyListener);
		}
	}

	/**
	 * Begins a batch of operations.
	 * Forward notifications to messageNotifyListener
	 */
	public void beginOperation(ISVNClientAdapter aSvnClient, OperationProgressNotifyListener anOperationNotifyListener) {
		this.operationNotifyListener = anOperationNotifyListener;
		beginOperation(aSvnClient);
	}
	
	public void endOperation(boolean refresh) throws SVNException {
		endOperation(refresh, null);
	}
	
	public void endOperation() throws SVNException {
		endOperation(true, null);
	}
	
	public void endOperation(boolean refresh, Set<IResource> refreshResourceList) throws SVNException {
		endOperation(refresh, refreshResourceList, true);
	}
	
	/**
	 * Ends a batch of operations. Pending changes are committed only when the
	 * number of calls to endOperation() balances those to beginOperation().
	 */
	public void endOperation(boolean refresh, Set<IResource> refreshResourceList, boolean refreshLocal) throws SVNException {
		try {
			if (lock.getNestingCount() == 1) {
				svnClient.removeNotifyListener(this);
				if (operationNotifyListener != null) {
					operationNotifyListener.clear(); //Clear progress information
					svnClient.setProgressListener(null);
				}
				if (refreshResourceList != null) {
					FilteringContainerList folderList = new FilteringContainerList(refreshResourceList);
					for (IContainer resource : folderList) {
						SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus((IContainer)resource, true);
					}
					IResource[] resources = new IResource[refreshResourceList.size()];
					refreshResourceList.toArray(resources);
					SVNProviderPlugin.broadcastModificationStateChanges(resources);
				}
				if (refreshLocal) {
					FilteringContainerList foldersToRefresh = new FilteringContainerList(localRefreshList);
					for (IContainer folder : foldersToRefresh) {
		                try {
		                	folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						} catch (CoreException e) {}
					}
				}
			}
		} finally {
			lock.release();
			operationNotifyListener = null;
			localRefreshList = new LinkedHashSet<IResource>();
		}
	}

	public void onNotify(File path, SVNNodeKind kind) {
		IPath pathEclipse = new Path(path.getAbsolutePath());
		IResource[] resources = SVNWorkspaceRoot.getResourcesFor(pathEclipse, false);
		for (IResource resource : resources) {			
			localRefreshList.add(resource);
		}
        
		if (operationNotifyListener != null)
		{
			operationNotifyListener.onNotify(path, kind);
			if ((operationNotifyListener.getMonitor() != null) && (operationNotifyListener.getMonitor().isCanceled()))
			{
				try {
					svnClient.cancelOperation();
				} catch (SVNClientException e) {
					SVNProviderPlugin.log(SVNException.wrapException(e));
				}
			}
		}
	}

	public void logCommandLine(String commandLine) {
	}

	public void logRevision(long revision, String path) {
	}

	public void logCompleted(String message) {
		if (operationNotifyListener != null)
		{
			operationNotifyListener.logMessage(message);
		}
	}

	public void logError(String message) {
	}

	public void logMessage(String message) {
		if (operationNotifyListener != null)
		{
			operationNotifyListener.logMessage(message);
		}
	}

	public void setCommand(int command) {
	}
}