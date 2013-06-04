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
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * UpdateAction performs a 'svn update' command on the selected resources.
 * If conflicts are present (file has been changed both remotely and locally),
 * the changes will be merged into the local file such that the user must
 * resolve the conflicts. 
 */
public class UpdateAction extends WorkbenchWindowAction {
	private IResource[] selectedResources;
	private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
	private boolean setDepth = false;
	protected boolean canRunAsJob = true;
	
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
        	IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
	        IResource[] resources = getSelectedResources(); 
	        SVNConflictResolver conflictResolver = new SVNConflictResolver(resources[0], store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TEXT_FILES), store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES), store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_PROPERTIES), store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS));
        	UpdateOperation updateOperation = new UpdateOperation(getTargetPart(), resources, SVNRevision.HEAD);
	    	updateOperation.setDepth(depth);
	    	updateOperation.setSetDepth(setDepth);
	    	updateOperation.setForce(store.getBoolean(ISVNUIConstants.PREF_UPDATE_TO_HEAD_ALLOW_UNVERSIONED_OBSTRUCTIONS));
	    	updateOperation.setIgnoreExternals(store.getBoolean(ISVNUIConstants.PREF_UPDATE_TO_HEAD_IGNORE_EXTERNALS));
	    	updateOperation.setCanRunAsJob(canRunAsJob);
	    	updateOperation.setConflictResolver(conflictResolver);
        	updateOperation.run();
        } 		
	}

	protected IResource[] getSelectedResources() {
		if (selectedResources == null) return super.getSelectedResources();
		else return selectedResources;
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("UpdateAction.updateerror"); //$NON-NLS-1$
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_UPDATE;
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setSetDepth(boolean setDepth) {
		this.setDepth = setDepth;
	}


	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}
	
}
