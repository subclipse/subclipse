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
import java.util.Arrays;
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
		List updateRuns = null;
		updateRuns = getAddedAndChangedResources(set);
		for (Iterator it = updateRuns.iterator(); it.hasNext();) {
			UpdateResourcesSet element = (UpdateResourcesSet) it.next();
		    new UpdateOperation(getPart(), element.getResources(), element.getRevision(), false).run(progress);			
		}
		updateRuns = getDeletedResources(set);
		for (Iterator it = updateRuns.iterator(); it.hasNext();) {
			UpdateResourcesSet element = (UpdateResourcesSet) it.next();
		    new UpdateOperation(getPart(), element.getResources(), element.getRevision(), false).run(progress);			
		}
	}
	
	/**
	 * Collect the "not to be deleted" incoming changes.
	 * Sort them ascending, so incoming dirs are created soon than incoming files from within.
	 * Groups them by the repositoryRevisions
	 * @param set
	 * @return List with UpdateResourcesSet objects.
	 */
	private List getAddedAndChangedResources(SyncInfoSet set)
	{
		SyncInfo[] infos = set.getSyncInfos();
		Map resourceGroups = new HashMap();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (SyncInfo.getChange(info.getKind()) != SyncInfo.DELETION)
			{
				List resources = (List) resourceGroups.get(((SVNStatusSyncInfo) info).getRepositoryRevision());
				if (resources == null) {
					resources = new ArrayList();
					resourceGroups.put(((SVNStatusSyncInfo) info).getRepositoryRevision(), resources);
				}				
				resources.add(info.getLocal());
			}
		}
		return UpdateResourcesSet.convertFactorizingMap(resourceGroups, true);
	}

	/**
	 * Collect the "to be deleted" incoming changes.
	 * Sort them descending, so incoming dir deletions are deleted only after the files from within are deleted.
	 * Groups them by the repositoryRevisions
	 * @param set
	 * @return
	 */
	private List getDeletedResources(SyncInfoSet set)
	{
		SyncInfo[] infos = set.getSyncInfos();
		Map resourceGroups = new HashMap();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (SyncInfo.getChange(info.getKind()) == SyncInfo.DELETION)
			{
				List resources = (List) resourceGroups.get(((SVNStatusSyncInfo) info).getRepositoryRevision());
				if (resources == null) {
					resources = new ArrayList();
					resourceGroups.put(((SVNStatusSyncInfo) info).getRepositoryRevision(), resources);
				}				
				resources.add(info.getLocal());
			}
		}
		return UpdateResourcesSet.convertFactorizingMap(resourceGroups, false);
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
     */
    protected boolean canRunAsJob() {
        return true;
    }
    
    private static class UpdateResourcesSet {
    	private SVNRevision revision = null;
    	private IResource[] resources = null;
    	
    	protected static List convertFactorizingMap(Map resourceGroups, final boolean ascending)
    	{
    		List result = new ArrayList(resourceGroups.size());
    		for (Iterator it = resourceGroups.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				result.add(new UpdateResourcesSet((SVNRevision) entry.getKey(), (List) entry.getValue(), ascending));
			}
    		return result;
    	}
    	
    	protected UpdateResourcesSet(final SVNRevision revision, final List resourcesList, final boolean ascending)
    	{
    		this.revision = revision;    		
    		this.resources = (IResource[]) resourcesList.toArray(new IResource[resourcesList.size()]);
            Arrays.sort(this.resources, new Comparator() {
    			public int compare(Object o1, Object o2) {
    				return ((IResource) o1).getFullPath().toString().compareTo(((IResource) o2).getFullPath().toString())
    				* ((ascending) ? 1 : -1);
    			}});
    	}
    	
		protected IResource[] getResources() {
			return resources;
		}
		protected SVNRevision getRevision() {
			return revision;
		}
    }
}
