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
package org.tigris.subversion.subclipse.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.conflicts.TreeConflictsView;

public class SVNMarkerListener implements IResourceStateChangeListener {
	private boolean treeConflictsViewRefreshed;

    public SVNMarkerListener() {
        super();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
     */
    public void resourceSyncInfoChanged(IResource[] changedResources) {
        for (int i = 0; i < changedResources.length; i++) {
    	    try {
    	    	if (changedResources[i].exists())
    	    	{
    	    		changedResources[i].deleteMarkers("org.tigris.subversion.subclipse.ui.conflictMarker", true, IResource.DEPTH_ZERO); //$NON-NLS-1$
    	    		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(changedResources[i]);
    	    		LocalResourceStatus status = svnResource.getStatus();
    	    		if (status.isTextConflicted()  || status.isPropConflicted() || status.hasTreeConflict()) {
    	    			try {
    	    				IMarker marker = changedResources[i].createMarker("org.tigris.subversion.subclipse.ui.conflictMarker"); //$NON-NLS-1$
    	    				setMessage(status, marker); 
    	    				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    	    				marker.setAttribute("textConflict", status.isTextConflicted()); //$NON-NLS-1$
    	    				marker.setAttribute("propertyConflict", status.isPropConflicted()); //$NON-NLS-1$
    	    				marker.setAttribute("treeConflict", status.hasTreeConflict()); //$NON-NLS-1$
    	    			} catch (Exception e) {
    	    				SVNUIPlugin.log(e.getMessage());
    	    			}
    	    		}
    	    	}
    	    } catch (Exception e) {
    	        SVNUIPlugin.log(e.getMessage());
    	    }
        }
        if (!treeConflictsViewRefreshed) {
        	treeConflictsViewRefreshed = TreeConflictsView.refresh(changedResources);
        }
    }

	private void setMessage(LocalResourceStatus status, IMarker marker)
			throws CoreException {
		int count = 0;
		if (status.isTextConflicted()) count++;
		if (status.isPropConflicted()) count++;
		if (status.hasTreeConflict()) count++;
		StringBuffer message = new StringBuffer(Policy.bind("SVNConflicts") + " ("); //$NON-NLS-1$ //$NON-NLS-2$
		if (status.isTextConflicted()) {
			message.append("Text"); //$NON-NLS-1$
			if (count == 2) message.append(" and "); //$NON-NLS-1$
			if (count == 3) message.append(", "); //$NON-NLS-1$
		}
		if (status.isPropConflicted()) {
			message.append("Property"); //$NON-NLS-1$
			if (count == 3) message.append(" and "); //$NON-NLS-1$
		}
		if (status.hasTreeConflict()) {
			message.append("Tree"); //$NON-NLS-1$
		}
		if (count == 1) message.append(" Conflict"); //$NON-NLS-1$
		else message.append(" Conflicts"); //$NON-NLS-1$
		message.append(")"); //$NON-NLS-1$
		marker.setAttribute(IMarker.MESSAGE, message.toString());
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceModified(org.eclipse.core.resources.IResource[])
     */
    public void resourceModified(IResource[] changedResources) {

    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
     */
    public void projectConfigured(IProject project) {

    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
     */
    public void projectDeconfigured(IProject project) {

    }
    
    public void initialize() {
    	treeConflictsViewRefreshed = false;
    }

}
