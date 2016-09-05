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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.ChangeSetDiffNode;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class RevertSynchronizeAction extends SynchronizeModelAction {
	private String url;
	@SuppressWarnings("rawtypes")
	private HashMap statusMap;

    public RevertSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
        super(text, configuration);
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			@SuppressWarnings("rawtypes")
			public boolean select(SyncInfo info) {
				SyncInfoDirectionFilter outgoingFilter = new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING, SyncInfo.CONFLICTING});
			    if (!outgoingFilter.select(info)) return false;
			    IStructuredSelection selection = getStructuredSelection();
			    Iterator iter = selection.iterator();
			    boolean removeUnAdded  = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE);
			    
			    while (iter.hasNext()) {
			    	ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
			    	IResource resource = element.getResource();
			    	if (resource == null) continue;
			    	if (resource.isLinked()) return false;
			    	if(!removeUnAdded)
			    	{
	                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);			    
	                try {
	                	if (!svnResource.isManaged()) return false;
	                } catch (SVNException e) {
	                    return false;
	                }
			    	}
			    }
             return true;
			}
		};
	}    

    @SuppressWarnings({ "rawtypes", "unchecked" })
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		statusMap = new HashMap();
    	url = null;
	    IStructuredSelection selection = getStructuredSelection();
	    if (selection.size() == 1) {
	        ISynchronizeModelElement element = (ISynchronizeModelElement)selection.getFirstElement();
		    IResource resource = element.getResource();
		    if (resource != null) {
			    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
	            try {
	                url = svnResource.getStatus().getUrlString();
	                if ((url == null) || (resource.getType() == IResource.FILE)) url = Util.getParentUrl(svnResource);
	            } catch (SVNException e) {
	            	SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
	            }
		    }
	    }
		List selectedResources = new ArrayList(elements.length);
		for (int i=0; i<elements.length; i++) {
			if (elements[i] instanceof ISynchronizeModelElement) {
				selectedResources.add(((ISynchronizeModelElement)elements[i]).getResource());
			}
		}
		IResource[] resources = new IResource[selectedResources.size()];
		selectedResources.toArray(resources);
		
		boolean changeSetMode = isChangeSetMode();
		List<IResource> topSelection = new ArrayList<IResource>();
		if (!changeSetMode) {
			Iterator iter = selection.iterator();
			while (iter.hasNext()) {
				ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
				topSelection.add(element.getResource());
			}
		}
		IResource[] topSelectionArray;
		if (changeSetMode) {
			topSelectionArray = resources;
		}
		else {
			topSelectionArray = new IResource[topSelection.size()];
			topSelection.toArray(topSelectionArray);
		}
		
		RevertSynchronizeOperation revertOperation = new RevertSynchronizeOperation(configuration, elements, url, resources, statusMap);
		revertOperation.setSelectedResources(topSelectionArray);
		return revertOperation;
    }
    
    private boolean isChangeSetMode() {
        Viewer viewer = getConfiguration().getPage().getViewer();
        if (viewer instanceof TreeViewer) {
        	TreeItem[] items = ((TreeViewer)viewer).getTree().getItems();
        	for (TreeItem item : items) {
        		if (item.getData() instanceof ChangeSetDiffNode) {
        			return true;
        		}
        	}
        }
        return false;
    }

}
