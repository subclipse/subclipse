/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Sync view operation for getting file system resources
 */
public class UpdateSynchronizeOperation extends SVNSynchronizeOperation {

	protected UpdateSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#promptForConflictHandling(org.eclipse.swt.widgets.Shell, org.eclipse.team.core.synchronize.SyncInfoSet)
	 */
	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		// If there is a conflict in the syncSet, we need to prompt the user before proceeding.
		if (syncSet.hasConflicts() || syncSet.hasOutgoingChanges()) {
			switch (promptForConflicts(shell, syncSet)) {
			case 0:
				// Yes, synchronize conflicts as well
				break;
			case 1:
				// No, remove outgoing
				syncSet.removeConflictingNodes();
				syncSet.removeOutgoingNodes();
				break;
			case 2:
			default:
				// Cancel
				return false;
			}	
		}
		return true;
	}

	/**
	 * Prompts the user to determine how conflicting changes should be handled.
	 * Note: This method is designed to be overridden by test cases.
	 * @return 0 to sync conflicts, 1 to sync all non-conflicts, 2 to cancel
	 */
	private int promptForConflicts(Shell shell, SyncInfoSet syncSet) {
		String[] buttons = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
		String title = Policy.bind("SyncAction.update.conflict.title"); //$NON-NLS-1$
		String question = Policy.bind("SyncAction.update.conflict.question"); //$NON-NLS-1$
		final MessageDialog dialog = new MessageDialog(shell, title, null, question, MessageDialog.QUESTION, buttons, 0);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		return dialog.getReturnCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#run(org.eclipse.team.examples.filesystem.FileSystemProvider, org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor progress) throws InvocationTargetException, InterruptedException {
		//The resources to be updated are factorized and sorted first.
		//They are grouped by revision numbers (mostly necessary for svn:externals)
		//They are sorted ascending or descending for additions res. deletions.
		Collection updateRuns = getToBeUpdatedResources(set);
		for (Iterator it = updateRuns.iterator(); it.hasNext();) {
			UpdateResourcesSet element = (UpdateResourcesSet) it.next();
		    new UpdateOperation(getPart(), element.getRecursiveResourceUpdates(), element.getRevision(), true).run(progress);
		    new UpdateOperation(getPart(), element.getNonRecursiveResourceUpdates(), element.getRevision(), false).run(progress);
		}
	}
	
	/**
	 * Collect and group incoming changes.
	 * Group them to three groups - deletions, additions and changes.
	 * Sort deletion descending, so incoming dir deletions are deleted only after the files from within are deleted.
	 * Sort additions ascending, so incoming dirs are created soon than incoming files from within.
	 * Sort order of changes is irrelevant (ascending).
	 * Groups them by the repositoryRevisions
	 * @param set
	 * @return List with UpdateResourcesSet objects ordered by revision numbers.
	 */
	private Collection getToBeUpdatedResources(SyncInfoSet set)
	{
		SyncInfo[] infos = set.getSyncInfos();
		Map resourceGroups = new HashMap();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			SVNRevision revision = ((SVNStatusSyncInfo) info).getRepositoryRevision();
			UpdateResourcesSet resourceSet = (UpdateResourcesSet) resourceGroups.get(revision);
			if (resourceSet == null) {
				resourceSet = new UpdateResourcesSet(revision);
				resourceGroups.put(revision, resourceSet);
			}				
			resourceSet.addResource(info);
		}
		return resourceGroups.values();
	}

	/* (non-Javadoc)
     * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
     */
    protected boolean canRunAsJob() {
        return true;
    }
    
    /**
     * Helper class for grouping storing and sorting resources scheduled for update 
     */
    private static class UpdateResourcesSet {
    	private SVNRevision revision = null;   	
    	private List addedResources = new ArrayList();
    	private List changedResources = new ArrayList();
    	private List deletedFiles = new ArrayList();
    	private List deletedDirectories = new ArrayList();

    	protected UpdateResourcesSet(final SVNRevision revision)
    	{
    		this.revision = revision;
    	}

    	protected void addResource(SyncInfo info) {
			if (SyncInfo.getChange(info.getKind()) == SyncInfo.DELETION)
			{
				if (IResource.FOLDER == info.getLocal().getType()) {
					deletedDirectories.add(info.getLocal());	
				} else {
					deletedFiles.add(info.getLocal());
				}
				
			}
			else if (SyncInfo.getChange(info.getKind()) == SyncInfo.ADDITION)
			{
				addedResources.add(info.getLocal());
			}
			else
			{
				changedResources.add(info.getLocal());
			}
    	}

    	/**
    	 * Sort additions ascending, so incoming dirs are created soon than incoming files from within.
    	 * @return List of resources to be deleted.
    	 */
    	private List getAddedResources()
    	{
    		Collections.sort(addedResources, new Comparator() {
    			public int compare(Object o1, Object o2) {
    				return ((IResource) o1).getFullPath().toString().compareTo(((IResource) o2).getFullPath().toString());
    			}});
    		return addedResources;
    	}

    	/**
    	 * Sort order of changes is irrelevant (ascending).
    	 * @return List of resources to be updated
    	 */
    	private List getChangedResources()
    	{
    		Collections.sort(changedResources, new Comparator() {
    			public int compare(Object o1, Object o2) {
    				return ((IResource) o1).getFullPath().toString().compareTo(((IResource) o2).getFullPath().toString());
    			}});
    		return changedResources;
    	}

    	/**
    	 * Sort deletion descending.
    	 * Originally thought that incoming dir deletions are deleted only after the files from within are deleted.
    	 * However directories are not deleted this way anymore. Directory deletions have to be performed always
    	 * in with recursive flag.
    	 * @return List of resources to be deleted
    	 */
    	private List getDeletedFiles()
    	{
    		Collections.sort(deletedFiles, new Comparator() {
    			public int compare(Object o1, Object o2) {
    				return ((IResource) o1).getFullPath().toString().compareTo(((IResource) o2).getFullPath().toString()) * -1;
    			}});
    		return deletedFiles;
    	}

    	/**
    	 * Sort deletion descending.
    	 * @return List of directories to be deleted
    	 */    	
    	private List getDeletedDirectories()
    	{
    		Collections.sort(deletedDirectories, new Comparator() {
    			public int compare(Object o1, Object o2) {
    				return ((IResource) o1).getFullPath().toString().compareTo(((IResource) o2).getFullPath().toString()) * -1;
    			}});
    		return deletedDirectories;
    	}

    	/**
    	 * Get the resources to be updated non-recursively.
    	 * They are sorted according to their nature.
    	 * @return
    	 */
		protected IResource[] getNonRecursiveResourceUpdates() {
			List allResources = new ArrayList();
			allResources.addAll(getDeletedFiles());			
			allResources.addAll(getAddedResources());			
			allResources.addAll(getChangedResources());			
			return (IResource[]) allResources.toArray(new IResource[allResources.size()]);
		}

    	/**
    	 * Get the resources to be updated recursively.
    	 * Actually the directory deletions.
    	 * @return
    	 */
		protected IResource[] getRecursiveResourceUpdates() {
			List allResources = getDeletedDirectories();
			return (IResource[]) allResources.toArray(new IResource[allResources.size()]);
		}

		protected SVNRevision getRevision() {
			return revision;
		}
    }
}
