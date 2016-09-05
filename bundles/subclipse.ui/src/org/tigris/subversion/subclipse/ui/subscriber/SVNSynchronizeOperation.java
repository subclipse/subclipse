/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Override SynchronizeModelOperation in order to delegate the operation to each file system 
 * provider instance (i.e. each project). Also, prompt to prune conflicts from the set of
 * selected resources.
 */
public abstract class SVNSynchronizeOperation extends SynchronizeModelOperation {

	protected SVNSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		// First, ask the user if they want to include conflicts
		SyncInfoSet syncSet = getSyncInfoSet();
		if (!promptForConflictHandling(getShell(), syncSet)) return;
		// Divide the sync info by project
		final Map projectSyncInfos = getProjectSyncInfoSetMap(syncSet);
		monitor.beginTask(null, projectSyncInfos.size() * 100);
		for (Iterator iter = projectSyncInfos.keySet().iterator(); iter.hasNext(); ) {
			final IProject project = (IProject) iter.next();

			// Pass the scheduling rule to the synchronizer so that sync change events
			// and cache commits to disk are batched
			SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(project, SVNUIPlugin.PROVIDER_ID);
			if (provider != null) {
				run(provider, (SyncInfoSet)projectSyncInfos.get(project), Policy.subMonitorFor(monitor,100));
			}
		}
		monitor.done();
	}

	/**
	 * Prompt the user to include conflicts. If the user choses not to include
	 * conflicts, they will be removed from the passed set. If the user cancels,
	 * <code>false</code> is returned.
	 * @param shell a shell
	 * @param syncSet the set of selected resources
	 * @return whether the operation should proceed.
	 */
	protected abstract boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet);

	/*
	 * Divide the sync info for the operation by project
	 */
	protected Map getProjectSyncInfoSetMap(SyncInfoSet syncSet) {
		Map map = new HashMap();
		SyncInfo[] infos = syncSet.getSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			IProject project = info.getLocal().getProject();
			SyncInfoSet set = (SyncInfoSet)map.get(project);
			if (set == null) {
				set = new SyncInfoSet();
				map.put(project, set);
			}
			set.add(info);
		}
		return map;
	}

    /**
     * @param IResource[] selectedResources
     * @param SyncInfoSet selections
     * @return resources that belong to current project
     * 
     * The Synch view runs operations once for each selected
     * project.  selectedResources contains all of the selected resources
     * across projects so we need to reduce the array to just the selected 
     * resources associated with the SyncInfoSet.  To do that we get the
     * IProject associated with the first resource in the set, and then
     * create an array from resources of just those resources that belong
     * to the same IProject.
     * 
     */	
	protected IResource[] extractResources(IResource[] selectedResources, SyncInfoSet set) {
        IResource[] setResources = set.getResources();
		IProject project = setResources[0].getProject();
		ArrayList projectResources = new ArrayList();
		for (int i = 0; i < selectedResources.length; i++) {
			if (selectedResources[i].getProject().equals(project)) projectResources.add(selectedResources[i]);
		}
		IResource[] resourceArray = new IResource[projectResources.size()];
		projectResources.toArray(resourceArray);
        return resourceArray;		
	}
	
	/**
	 * Run the operation on the sync info in the given set. The sync info will be all
	 * from the same project.
	 * @param provider
	 * @param set the sync info set
	 * @param monitor a progress monitor
	 */
	protected abstract void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor)  throws InvocationTargetException, InterruptedException;
}
