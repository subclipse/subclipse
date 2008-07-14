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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
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

	private Set changedResources = new LinkedHashSet();

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
		if (operationNotifyListener != null)
			aSvnClient.setProgressListener(operationNotifyListener);
		if (lock.getNestingCount() == 1) {
			changedResources.clear();
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
	
	/**
	 * Ends a batch of operations. Pending changes are committed only when the
	 * number of calls to endOperation() balances those to beginOperation().
	 */
	public void endOperation() throws SVNException {
		try {
			if (lock.getNestingCount() == 1) {
				svnClient.removeNotifyListener(this);
				if (operationNotifyListener != null) {
					operationNotifyListener.clear(); //Clear progress information
					svnClient.setProgressListener(null);
				}
				for (Iterator it = changedResources.iterator(); it.hasNext();) {
					IResource resource = (IResource) it.next();
					//Ensure the .svn has the team private flag set before refresh. 
					if (resource instanceof IContainer)
						handleSVNDir((IContainer) resource);
                    try {
                        // .svn directory will be refreshed so all files in the
                        // directory including resource will
                        // be refreshed later (@see SyncFileChangeListener)
                        resource.refreshLocal(IResource.DEPTH_INFINITE,new NullProgressMonitor());
                        if(Policy.DEBUG_METAFILE_CHANGES) {
                            System.out.println("[svn]" + SVNProviderPlugin.getPlugin().getAdminDirectoryName() + " dir refreshed : " + resource.getFullPath()); //$NON-NLS-1$
                        }
                        // Refreshing the root directory at this point will
                        // avoid problems with linked source folders.
                        if (resource.getParent().getType() == IResource.PROJECT)
                            resource.getParent().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
                    } catch (CoreException e) {
                        throw SVNException.wrapException(e);
                    }                    
				}
			}
		} finally {
			if (lock.getNestingCount() == 1)
				changedResources.clear();
			lock.release();
			operationNotifyListener = null;
		}
	}

	public void onNotify(File path, SVNNodeKind kind) {
		IPath pathEclipse = new Path(path.getAbsolutePath());

        if (kind == SVNNodeKind.UNKNOWN)  { // delete, revert 
            IPath pathEntries = pathEclipse.removeLastSegments(1).append(
            		SVNProviderPlugin.getPlugin().getAdminDirectoryName());
            changedResources.addAll(Arrays.asList(SVNWorkspaceRoot.getResourcesFor(pathEntries, false)));
            
            if (path.isDirectory()) {
            	IResource[] resources = SVNWorkspaceRoot.getResourcesFor(pathEclipse, false);
            	for (int i = 0; i < resources.length; i++) {
            		IResource resource = resources[i];
					if (resource.getType() != IResource.ROOT) {
						IResource svnDir = ((IContainer) resource).getFolder(new Path(
	                    		SVNProviderPlugin.getPlugin().getAdminDirectoryName()));
	                    changedResources.add(svnDir);
					}
            	}
            }
        }
        else
        {
			IResource svnDir = null;
			if (kind == SVNNodeKind.DIR) {
				// when the resource is a directory, two .svn directories can
				// potentially
				// be modified
				IResource[] resources = SVNWorkspaceRoot.getResourcesFor(pathEclipse, false);
				for (int i = 0; i < resources.length; i++) {
					IResource resource = resources[i];
					if (resource.getType() != IResource.ROOT) {
						if (resource.getProject() != resource) {
							// if resource is a project. We can't refresh ../.svn
							svnDir = resource.getParent().getFolder(
									new Path(SVNProviderPlugin.getPlugin().getAdminDirectoryName()));
							changedResources.add(svnDir);
						}
						if (resource instanceof IContainer) {
		                    svnDir = ((IContainer) resource).getFolder(new Path(
		                    		SVNProviderPlugin.getPlugin().getAdminDirectoryName()));
		                    changedResources.add(svnDir);
						}
					}
				}
			} else if (kind == SVNNodeKind.FILE) {
				IResource[] resources = SVNWorkspaceRoot.getResourcesFor(pathEclipse, false);

				for (int i = 0; i < resources.length; i++) {
					svnDir = resources[i].getParent().getFolder(
							new Path(SVNProviderPlugin.getPlugin().getAdminDirectoryName()));
					changedResources.add(svnDir);
				}
			}
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