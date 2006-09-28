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
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class UpdateSynchronizeOperation extends SVNSynchronizeOperation {
	private IResource[] resources;
	
	public UpdateSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IResource[] resourceArray = trimResources(extractResources(resources, set));
		SVNRevision revision = null;
		SyncInfo[] syncInfos = set.getSyncInfos();
		boolean containsDeletes = false;
		for (int i = 0; i < syncInfos.length; i++) {
			SVNStatusSyncInfo syncInfo = (SVNStatusSyncInfo)syncInfos[i];
			IResourceVariant remote = syncInfo.getRemote();
			if (remote != null && remote instanceof ISVNRemoteResource) {
				if (syncInfo.getRemoteResourceStatus() != null && syncInfo.getRemoteResourceStatus().getTextStatus() == SVNStatusKind.DELETED) {
					containsDeletes = true;
					continue;
				} 
				SVNRevision rev = ((ISVNRemoteResource)remote).getLastChangedRevision();
				if (rev instanceof SVNRevision.Number) {
					long nbr = ((SVNRevision.Number)rev).getNumber();
					if (revision == null) revision = rev;
					else {
						long revisionNumber = ((SVNRevision.Number)revision).getNumber();
						if (nbr > revisionNumber) revision = rev;
					}
				}
			}
		}
		if (revision == null || containsDeletes) revision = SVNRevision.HEAD;
		new UpdateOperation(getPart(), resourceArray, revision, true).run();
	}
	
	/**
	 * This method takes the array of resources to be updated and removes any
	 * items that have a parent folder that is also being updated, since the
	 * recursive update of a parent folder will cause the resource to be updated
	 * anyway.  This will make the update run faster.
	 * @param resourceArray
	 * @return
	 */
    private IResource[] trimResources(IResource[] resourceArray) {
    	// Get a list of just the folders.
        List folders = new ArrayList();
        for (int i = 0; i < resourceArray.length; i++) {
            if (resourceArray[i].getType() == IResource.FOLDER || resourceArray[i].getType() == IResource.PROJECT) 
                folders.add(resourceArray[i]);
        }
        
        List trimmedList = new ArrayList();
        for (int i = 0; i < resourceArray.length; i++) {
            if (!parentIncluded(resourceArray[i], folders))
                trimmedList.add(resourceArray[i]);
        }
        
        IResource[] trimmedArray = new IResource[trimmedList.size()];
		trimmedList.toArray(trimmedArray);
		return trimmedArray;
    }
    
    private boolean parentIncluded(IResource resource, List folders) {
        IResource parent = resource.getParent();
        if (parent == null) return false;
        if (folders.contains(parent)) return true;
        return parentIncluded(parent, folders);
    }
}
