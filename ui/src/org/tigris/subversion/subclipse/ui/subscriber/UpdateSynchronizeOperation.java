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
		IResource[] resourceArray = extractResources(resources, set);
		SVNRevision revision = null;
		// If any folders are selected, we will update to HEAD so as
		// to not back-revision any committed files which could be at
		// a higher rev than the selected files
		for (int i = 0; i < resourceArray.length; i++) {
            if (resourceArray[i].getType() != IResource.FILE) {
                revision = SVNRevision.HEAD;
                break;
            }
        }
		if (revision == null) {
			SyncInfo[] syncInfos = set.getSyncInfos();
			for (int i = 0; i < syncInfos.length; i++) {
				SVNStatusSyncInfo syncInfo = (SVNStatusSyncInfo)syncInfos[i];
				IResourceVariant remote = syncInfo.getRemote();
				if (remote != null && remote instanceof ISVNRemoteResource) {
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
		}
		if (revision == null) revision = SVNRevision.HEAD;
		new UpdateOperation(getPart(), resourceArray, revision, true).run();
	}

}
