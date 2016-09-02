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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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

/**
 * @author Martin Letenay (letenay at tigris dot org)
 */
public class IgnoreSynchronizeAction extends SynchronizeModelAction {

    public IgnoreSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
        super(text, configuration);
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			public boolean select(SyncInfo info) {
				SyncInfoDirectionFilter filter = new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING});
				if (!filter.select(info)) return false;
			    IStructuredSelection selection = getStructuredSelection();
		        ISynchronizeModelElement element = (ISynchronizeModelElement)selection.getFirstElement();
			    IResource resource = element.getResource();
		        if (resource == null) return false;
                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);			    
                try {
            	    //If the resource is a IProject then the action should not be enabled.
            	    if( svnResource.getIResource() instanceof IProject)
            	        return false;
            		// If the parent is not managed there is no way to set the svn:ignore property
            		if (!svnResource.getParent().isManaged()) {
            			return false;
            		}               	
                    return !svnResource.getStatus().isManaged() && resource.exists();
                } catch (SVNException e) {
                    return false;
                }
			}
		};
	}    

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
		return new IgnoreSynchronizeOperation(configuration, elements, resources);
    }

}
