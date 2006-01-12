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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Put action that appears in the synchronize view. It's main purpose is
 * to filter the selection and delegate its execution to the put operation.
 */
public class CommitSynchronizeAction extends SynchronizeModelAction {

	public CommitSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
	 */
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		ArrayList selectedElements = new ArrayList();
		IStructuredSelection selection = getStructuredSelection();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			ISynchronizeModelElement synchronizeModelElement = (ISynchronizeModelElement)iter.next();
			IResource resource = synchronizeModelElement.getResource();
			selectedElements.add(resource);
		}
		IResource[] resources = new IResource[selectedElements.size()];
		selectedElements.toArray(resources);
	    return new CommitSynchronizeOperation(configuration, elements, resources);
	}	
}
