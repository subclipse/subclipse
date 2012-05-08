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
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
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
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class OverrideAndUpdateSynchronizeAction extends SynchronizeModelAction {

	public OverrideAndUpdateSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}
	
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			public boolean select(SyncInfo info) {
				SyncInfoDirectionFilter filter = new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING,SyncInfo.CONFLICTING});
				if (!filter.select(info)) return false;
			    IStructuredSelection selection = getStructuredSelection();
			    boolean removeUnAdded  = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE);
			    Iterator iter = selection.iterator();
			    while (iter.hasNext()) {
			    	ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
			    	IResource resource = element.getResource();
			    	if (resource == null) {
			    		return false;
			    	}
			    	ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);	
			    	
			    	if (svnResource == null) {
			    		return false;
			    	}		    	
	                try {
	                	if (!resource.exists() && !svnResource.getStatusFromCache().isDeleted()) {
	                		return false;
	                	}
	                	if (svnResource.isAdded()) return false;
	                	if (!removeUnAdded && !svnResource.isManaged()) return false;
	                } catch (SVNException e) {
	                    return false;
	                }			    		
			    }
                return true;
			}
		};
	}

	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		List selectedResources = new ArrayList(elements.length);
		for (int i=0; i<elements.length; i++) {
			if (elements[i] instanceof ISynchronizeModelElement) {
				selectedResources.add(((ISynchronizeModelElement)elements[i]).getResource());
			}
		}
		IResource[] resources = new IResource[selectedResources.size()];
		selectedResources.toArray(resources);
		return new OverrideAndUpdateSynchronizeOperation(configuration, elements, resources, resources);
	}

}
