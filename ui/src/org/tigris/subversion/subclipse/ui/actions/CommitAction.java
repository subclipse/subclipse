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
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.repository.RepositoryManager;

/**
 * Action for checking in files to a subversion provider
 * Prompts the user for a release comment.
 */
public class CommitAction extends WorkspaceAction {
	
	/*
     * get non added resources and prompts for resources to be added
     * prompts for comments
     * add non added files
     * commit selected files
	 * @see SVNAction#execute(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final IResource[] resources = getSelectedResources();
		final RepositoryManager manager = SVNUIPlugin.getPlugin().getRepositoryManager();
		final String[] comment = new String[] {null};
		final IResource[][] resourcesToBeAdded = new IResource[][] { null };
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				try {
					// search for any non-added, non-ignored resources in the selection
					IResource[] unadded = getUnaddedResources(resources, monitor);
					resourcesToBeAdded[0] = promptForResourcesToBeAdded(manager, unadded);
					if (resourcesToBeAdded[0] == null) return;
					comment[0] = promptForComment(manager, resources);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		}, true /* cancelable */, PROGRESS_BUSYCURSOR); //$NON-NLS-1$
		
		if (comment[0] == null) return; // user canceled
		
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				try {
					// execute the add and commit in a single SVN runnable so sync changes are batched
					SVNProviderPlugin.run(
						new ISVNRunnable() {
							public void run(IProgressMonitor monitor) throws SVNException {
								try {
									int ticks=100;
									monitor.beginTask(null, ticks);
									if (resourcesToBeAdded[0].length > 0) {
										int addTicks = 20;
										manager.add(resourcesToBeAdded[0], Policy.subMonitorFor(monitor, addTicks));
										ticks-=addTicks;
									}
									IResource[] shared = getSharedResources(resources);
									if (shared.length == 0) return;
									manager.commit(shared, comment[0], Policy.subMonitorFor(monitor,ticks));
								} catch (TeamException e) {
									throw SVNException.wrapException(e);
								}
							}
						}, monitor);
				} catch (SVNException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		}, true /* cancelable */, PROGRESS_DIALOG); //$NON-NLS-1$
	}

	/**
	 * get the unadded resources in resources parameter
	 */
	private IResource[] getUnaddedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
		final List unadded = new ArrayList();
		final SVNException[] exception = new SVNException[] { null };
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
            if (resource.exists()) {
			    // visit each resource deeply
			    try {
				    resource.accept(new IResourceVisitor() {
					public boolean visit(IResource resource) throws CoreException {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						// skip ignored resources and their children
						try {
							if (svnResource.isIgnored())
								return false;
							// visit the children of shared resources
							if (svnResource.isManaged())
								return true;
						} catch (SVNException e) {
							exception[0] = e;
						}
						// don't add folders to avoid comitting empty folders
						if (resource.getType() == IResource.FOLDER)
							return true;
						// file is unshared so record it
						unadded.add(resource);
						// no need to go into children because add is deep
						return false;
					}
				}, IResource.DEPTH_INFINITE, false /* include phantoms */);
			    } catch (CoreException e) {
				    throw SVNException.wrapException(e);
			    }
			    if (exception[0] != null) throw exception[0];
            }
		}
		return (IResource[]) unadded.toArray(new IResource[unadded.size()]);
	}


	/*
	 * Return all resources in the provided collection that are shared with a repo
	 * @param resources
	 * @return IResource[]
	 */
	private IResource[] getSharedResources(IResource[] resources) throws SVNException {
		List shared = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (svnResource.isManaged()) { 
			  	shared.add(resource);
			}
		}
		return (IResource[]) shared.toArray(new IResource[shared.size()]);
	}
	
	/**
	 * Prompts the user for a release comment.
	 * @return the comment, or null to cancel
	 */
	protected String promptForComment(RepositoryManager manager, IResource[] resourcesToCommit) {
		return manager.promptForComment(getShell(), resourcesToCommit);
	}
	
    /**
     * Prompt to add all or some of the provided resources to version control.
     * The value null is returned if the dialog is cancelled.
     */
	private IResource[] promptForResourcesToBeAdded(RepositoryManager manager, IResource[] unadded) {
		return manager.promptForResourcesToBeAdded(getShell(), unadded);
	}
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("CommitAction.commitFailed"); //$NON-NLS-1$
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}
    
    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForInaccessibleResources()
     */
    protected boolean isEnabledForInaccessibleResources() {
        return true;
    }
}
