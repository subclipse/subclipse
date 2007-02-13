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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils;

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
			    	if (resource == null || !resource.exists()) return false;
			    	ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);			    
	                try {
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
		IStructuredSelection selection = getStructuredSelection();
	    ArrayList selectedElements = new ArrayList();
	    Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			ISynchronizeModelElement synchronizeModelElement = (ISynchronizeModelElement)iter.next();
			IResource resource = synchronizeModelElement.getResource();
			selectedElements.add(resource);
		}
		IResource[] resources = new IResource[selectedElements.size()];
		selectedElements.toArray(resources); 
		IResource[] modifiedResources = null;
		try {
			modifiedResources = getModifiedResources(resources, new NullProgressMonitor());					
		} catch (SVNException e) {
            
        }
		return new OverrideAndUpdateSynchronizeOperation(configuration, elements, modifiedResources, resources);
	}
	
	private IResource[] getModifiedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
	    final List modified = new ArrayList();
	    for (int i = 0; i < resources.length; i++) {
			 IResource resource = resources[i];
			 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			 
			 // get adds, deletes, updates and property updates.
			 GetStatusCommand command = new GetStatusCommand(svnResource, true, false);
			 command.run(iProgressMonitor);
			 ISVNStatus[] statuses = command.getStatuses();
			 for (int j = 0; j < statuses.length; j++) {
			     if (SVNStatusUtils.isReadyForRevert(statuses[j])  ||
			   		  !SVNStatusUtils.isManaged(statuses[j])) {
			         IResource currentResource = SVNWorkspaceRoot.getResourceFor(statuses[j]);
			         if (currentResource != null)
			             modified.add(currentResource);
			     }
			 }
		}
	    return (IResource[]) modified.toArray(new IResource[modified.size()]);
	}

}
