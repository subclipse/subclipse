/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.sync;

 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.ILocalSyncElement;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ui.sync.CatchupReleaseViewer;
import org.eclipse.team.internal.ui.sync.SyncCompareInput;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * defines methods for the following sequence steps:
 * <LI>running a lengthy compare operation under progress monitor control,
 * <LI>creating a UI for displaying the model and initializing the some widgets with the compare result,
 * <LI>tracking the dirty state of the model in case of merge,
 * <LI>saving the model.  
 */
public class SVNSyncCompareInput extends SyncCompareInput {

	private IResource[] resources;
	private boolean onlyOutgoing = false;
	
	public SVNSyncCompareInput(IResource[] resources) {
		this(resources, false);
	}
	
//	protected SVNSyncCompareInput(IResource[] resources, int granularity) {
//		super(granularity);
//		this.resources = getNonOverlapping(resources);
//	}
	
	public SVNSyncCompareInput(IResource[] resources, boolean onlyOutgoing) {
		super(SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_CONSIDER_CONTENTS) ? ILocalSyncElement.GRANULARITY_CONTENTS : ILocalSyncElement.GRANULARITY_TIMESTAMP);
		this.onlyOutgoing = onlyOutgoing;
		this.resources = getNonOverlapping(resources);		
	}
	
	/**
	 * Method getNonOverlapping ensures that a resource is not covered more than once.
	 * @param resources
	 * @return IResource[]
	 */
	private IResource[] getNonOverlapping(IResource[] resources) {
		// Sort the resources so the shortest paths are first
		List sorted = new ArrayList();
		sorted.addAll(Arrays.asList(resources));
		Collections.sort(sorted, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				IResource resource0 = (IResource) arg0;
				IResource resource1 = (IResource) arg1;
				return resource0.getFullPath().segmentCount() - resource1.getFullPath().segmentCount();
			}
			public boolean equals(Object arg0) {
				return false;
			}
		});
		// Collect all non-overlapping resources
		List coveredPaths = new ArrayList();
		for (Iterator iter = sorted.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			IPath resourceFullPath = resource.getFullPath();
			boolean covered = false;
			for (Iterator it = coveredPaths.iterator(); it.hasNext();) {
				IPath path = (IPath) it.next();
				if(path.isPrefixOf(resourceFullPath)) {
					covered = true;
				}
			}
			if (covered) {
				// if the resource is covered by a parent, remove it
				iter.remove();
			} else {
				// if the resource is a non-covered folder, add it to the covered paths
				if (resource.getType() == IResource.FOLDER) {
					coveredPaths.add(resource.getFullPath());
				}
			}
		}
		return (IResource[]) sorted.toArray(new IResource[sorted.size()]);
	}
	
	/**
	 * Overridden to create a custom DiffTreeViewer in the top left pane of the CompareProvider.
	 */
	public Viewer createDiffViewer(Composite parent) {
		CatchupReleaseViewer catchupReleaseViewer = new SVNCatchupReleaseViewer(parent, this);
		setViewer(catchupReleaseViewer);
		return catchupReleaseViewer;
	}

	/**
     * creates the sync elements
	 */
	protected IRemoteSyncElement[] createSyncElements(IProgressMonitor monitor) throws TeamException {
		// Ensure that the projects for all resources being synchronized exist
		// Note: this could happen on a refresh view after a synced project was deleted.
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (!resource.getProject().exists()) {
				throw new SVNException(Policy.bind("SVNSyncCompareInput.projectDeleted", resource.getProject().getName())); //$NON-NLS-1$
			}
		}
		
		monitor.beginTask(null, 1000 + (onlyOutgoing?10:0));
		IResource[] resourcesToSync;
		if (onlyOutgoing) {
			List filteredResources = Arrays.asList(resources);
			filteredResources = filterOutgoingChangesSet(filteredResources, Policy.subMonitorFor(monitor, 10));
			resourcesToSync = (IResource[]) filteredResources.toArray(new IResource[filteredResources.size()]);
		} else {
			resourcesToSync = resources;
		}
         
        IProgressMonitor monitor2 = Policy.subMonitorFor(monitor,1000);
        IRemoteSyncElement[] trees = new IRemoteSyncElement[resourcesToSync.length];
        int work = 1000 * resourcesToSync.length;
        monitor2.beginTask(null, work);
        try {
            for (int i = 0; i < trees.length; i++) {
                trees[i] = SVNWorkspaceRoot.getRemoteSync(resourcesToSync[i], /*null,*/ Policy.subMonitorFor(monitor2, 1000));
            }
        } finally {
            monitor2.done();
        }
        return trees;
        
	}


//	protected void updateView() {
//		// Update the view
//		if (getDiffRoot().hasChildren()) {
//			getViewer().refresh();
//		} else {
//			getViewer().setInput(null);
//		}
//		
//		// Update the status line
//		updateStatusLine();
//	}
//	
//	/**
//	 * Overridden to mark the source as merged.
//	 */
//	protected void compareInputChanged(ICompareInput source) {
//		super.compareInputChanged(source);
//		updateView();
//		
//		// prompt user with warning
//		Shell shell = getShell();
//		if(shell != null) {
//			// prompt
//			if(source instanceof TeamFile) {
//				TeamFile file = (TeamFile)source;						
//				int direction = file.getChangeDirection();
//				int type = file.getChangeType();
//				if(direction == IRemoteSyncElement.INCOMING ||
//				   direction == IRemoteSyncElement.CONFLICTING) {
//					promptForConfirmMerge(getShell());
//			    }
//			}
//		}
//	}
//	
//	/*
//	 * Helper method to get svn elements from the selection in the sync editor input
//	 */
//	public static SVNRemoteSyncElement getSyncElementFrom(Object node) {
//		SVNRemoteSyncElement element = null;
//		if (node instanceof TeamFile) {
//			element = (SVNRemoteSyncElement)((TeamFile)node).getMergeResource().getSyncElement();
//		} else if (node instanceof ChangedTeamContainer) {
//			element = (SVNRemoteSyncElement)((ChangedTeamContainer)node).getMergeResource().getSyncElement();
//		}
//		return element;
//	}
//	
//	/*
//	 * Returns the resources in this input.
//	 */
//	public IResource[] getResources() {
//		return resources;
//	}
//	
//	/*
//	 * Inform user that when changes are merged in the sync view that confirm
//	 * merge should be called to finish the merge.
//	 */
//	private void promptForConfirmMerge(final Shell shell) {
//		final IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
//		if(!store.getBoolean(ISVNUIConstants.PREF_PROMPT_ON_SAVING_IN_SYNC)) {
//			return;
//		};
//		shell.getDisplay().syncExec(new Runnable() {
//			public void run() {							
//				AvoidableMessageDialog dialog = new AvoidableMessageDialog(
//						shell,
//						Policy.bind("SVNSyncCompareInput.confirmMergeMessageTitle"),  //$NON-NLS-1$
//						null,	// accept the default window icon
//						Policy.bind("SVNSyncCompareInput.confirmMergeMessage"),  //$NON-NLS-1$
//						MessageDialog.INFORMATION, 
//						new String[] {IDialogConstants.OK_LABEL}, 
//						0);
//				dialog.open();		
//				if(dialog.isDontShowAgain()) {
//					store.setValue(ISVNUIConstants.PREF_PROMPT_ON_SAVING_IN_SYNC, false);
//				}																				
//			}
//		});
//	}
//	
//	/**
//	 * Wrap the input preparation in a SVN session run so open sessions will be reused and
//	 * file contents under the same remote root folder will be fetched using the same connection.
//	 * 
//	 * Also run with refresh prompting if one of the resources is out of sync with the local
//	 * file system.
//	 */
//	public Object prepareInput(IProgressMonitor pm) throws InterruptedException, InvocationTargetException {
//		final Object[] result = new Object[] { null };
//		final Exception[] exception = new Exception[] {null};
//		try {
//			Session.run(null, null, false, new ISVNRunnable() {
//				public void run(IProgressMonitor monitor) throws SVNException {
//					try {
//						SVNUIPlugin.runWithRefresh(getShell(), resources, new IRunnableWithProgress() {
//							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//								result[0] = SVNSyncCompareInput.super.prepareInput(monitor);
//							}
//						}, monitor);
//					} catch (InterruptedException e) {
//						exception[0] = e;
//					} catch (InvocationTargetException e) {
//						exception[0] = e;
//					}
//				}
//			}, pm);
//		} catch (SVNException e) {
//			throw new InvocationTargetException(e);
//		}
//		
//		if (exception[0] != null) {
//			if (exception[0] instanceof InvocationTargetException) {
//				throw (InvocationTargetException)exception[0];
//			} else {
//				throw (InterruptedException)exception[0];
//			}
//		}
//		
//		if (hasDifferences(result[0])) {
//			return result[0];
//		} else {
//			return null;
//		}
//	}
//
//	/**
//	 * The given object has differences if it is an IDiffElement with a change
//	 * or it is an IDiffContainer that contains differences.
//	 * 
//	 * @param object
//	 * @return boolean
//	 */
//	private boolean hasDifferences(Object object) {
//		if (object instanceof IDiffElement) {
//			IDiffElement element = (IDiffElement)object;
//			if (element.getKind() != Differencer.NO_CHANGE) return true;
//			if (object instanceof IDiffContainer) {
//				IDiffContainer container = (IDiffContainer) object;
//				if (container.hasChildren()) {
//					IDiffElement[] children = container.getChildren();
//					for (int i = 0; i < children.length; i++) {
//						IDiffElement child = children[i];
//						if (hasDifferences(child)) return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	private boolean hasIncomingChanges(ChangedTeamContainer container) {
//		IDiffElement[] children = container.getChildren();
//		for (int i = 0; i < children.length; i++) {
//			IDiffElement element = children[i];
//			int direction = element.getKind() & Differencer.DIRECTION_MASK;
//			if (direction == ITeamNode.CONFLICTING || direction == ITeamNode.INCOMING) {
//				return true;
//			}
//			if (element instanceof ChangedTeamContainer)  {
//				boolean hasIncomingChanges = hasIncomingChanges((ChangedTeamContainer)element);
//				if (hasIncomingChanges) return true;
//			}
//		}
//		return false;
//	}
//	
//	/*
//	 * Method copied from TeamAction. It should be put in a common place
//	 */
//	protected Map getProviderMapping(IResource[] resources) {
//		Map result = new HashMap();
//		for (int i = 0; i < resources.length; i++) {
//			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject());
//			List list = (List)result.get(provider);
//			if (list == null) {
//				list = new ArrayList();
//				result.put(provider, list);
//			}
//			list.add(resources[i]);
//		}
//		return result;
//	}
//
//	protected SyncSet getSyncSet(IStructuredSelection selection) {
//		return new SVNSyncSet(selection);
//	}
//
	/*
	 * Return the resources from the original list that are modified. 
	 */
	private List filterOutgoingChangesSet(List resources, IProgressMonitor monitor) throws SVNException {
		try {
			monitor.beginTask(null, 100 * resources.size());
			monitor.subTask(Policy.bind("SVNSyncCompareInput.filteringOutgoingChanges")); //$NON-NLS-1$
			List result = new ArrayList();
			for (Iterator iter = resources.iterator(); iter.hasNext();) {
				IResource resource = (IResource) iter.next();
				ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				if (!svnResource.isIgnored() && svnResource.isModified())
					result.add(resource);
			}
			return result;
		} finally {
			monitor.done();
		}
	}
    
    /*
     * Returns the resources in this input.
     */
    public IResource[] getResources() {
        return resources;
    }    
    
}
