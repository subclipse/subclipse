/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;


public class ResolveSynchronizeAction extends SynchronizeModelAction {

    public ResolveSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
        super(text, configuration);
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			public boolean select(SyncInfo info) {
			    IStructuredSelection selection = getStructuredSelection();
			    Iterator iter = selection.iterator();
			    while (iter.hasNext()) {
			        ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
			        if (element.getResource() instanceof IFile) {
		                ISVNLocalResource svnResource = SVNWorkspaceRoot
		                .getSVNResourceFor(element.getResource());	
		                try {
		                    if (svnResource.getStatus().isTextConflicted() || svnResource.getStatus().hasTreeConflict()) return true;
		                } catch (SVNException e) {}
			        }			        
			    }
                return false;
			}
		};
	}   

    protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
        return new ResolveSynchronizeOperation(configuration, elements);
    }

}
