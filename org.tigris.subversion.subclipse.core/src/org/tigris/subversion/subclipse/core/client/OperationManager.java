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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
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
	
	/**
	 * Ends a batch of operations. Pending changes are committed only when the
	 * number of calls to endOperation() balances those to beginOperation().
	 */
	public void endOperation(boolean refresh, Set<IResource> refreshResourceList) throws SVNException {
		try {
			if (lock.getNestingCount() == 1) {
				svnClient.removeNotifyListener(this);
				if (operationNotifyListener != null) {
					operationNotifyListener.clear(); //Clear progress information
					svnClient.setProgressListener(null);
				}
				if (refreshResourceList != null) {
					List<IResource> folderList = new ArrayList<IResource>();
					for (IResource resource : refreshResourceList) {
						IResource folder;
						if (resource instanceof IContainer) {
							folder = resource;
						}
						else {
							folder = resource.getParent();
						}
						if (!folderList.contains(folder)) {
							folderList.add(folder);
						}
					}
					for (IResource resource : folderList) {
						 SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus((IContainer)resource, true);
					}
					IResource[] resources = new IResource[refreshResourceList.size()];
					refreshResourceList.toArray(resources);
					SVNProviderPlugin.broadcastModificationStateChanges(resources);
				}
				Set<IResource> foldersToRefresh = new LinkedHashSet<IResource>();
				for (IResource resource : localRefreshList) {
					if (resource.getType() == IResource.FILE) {
						foldersToRefresh.add(resource.getParent());
					}
					else {
						foldersToRefresh.add(resource);
					}
				}
				for (IResource folder : foldersToRefresh) {
	                try {
						folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (CoreException e) {}
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
	
	protected boolean handleSVNDir(final IContainer svnDir) {
		if (!svnDir.exists() || !svnDir.isTeamPrivateMember()) 
		{
			try {
				// important to have both the refresh and setting of team-private in the
				// same runnable so that the team-private flag is set before other delta listeners 
				// sees the SVN folder creation.
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						if (!svnDir.exists()) {
							svnDir.refreshLocal(IResource.DEPTH_ZERO,new NullProgressMonitor());
						}
						if (svnDir.exists())
							svnDir.setTeamPrivateMember(true);			
						if(Policy.DEBUG_METAFILE_CHANGES) {
							System.out.println("[svn] found a new SVN meta folder, marking as team-private: " + svnDir.getFullPath()); //$NON-NLS-1$
						}
					} 
				}, svnDir.getParent(), IWorkspace.AVOID_UPDATE, null);
			} catch(CoreException e) {
				SVNProviderPlugin.log(SVNException.wrapException(svnDir, Policy.bind("OperationManager.errorSettingTeamPrivateFlag"), e)); //$NON-NLS-1$
			}
		}
		return svnDir.isTeamPrivateMember();
	}

}