/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.repository.model;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.FetchMembersOperation;

public class RemoteFolderElement extends SVNModelElement implements IDeferredWorkbenchAdapter {

	/**
	 * Overridden to append the version name to remote folders which
	 * have version tags and are top-level folders.
	 */
	public String getLabel(Object o) {
		if (!(o instanceof ISVNRemoteFolder)) return null;
		ISVNRemoteFolder folder = (ISVNRemoteFolder)o;
		return folder.getName();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof ISVNRemoteFolder)) return null;

		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}
	/**
	 * @see org.tigris.subversion.subclipse.ui.model.SVNModelElement#internalGetChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) throws TeamException {
		if (!(o instanceof ISVNRemoteFolder)) return new Object[0];
		return ((ISVNRemoteFolder)o).members(monitor);
	}

    public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
    	// If it's not a folder, return an empty array
		if (!(o instanceof ISVNRemoteFolder)) {
			collector.add(new Object[0], monitor);
		}
        try {
            monitor = Policy.monitorFor(monitor);
            monitor.beginTask(Policy.bind("RemoteFolderElement_fetchingRemoteMembers.message", getLabel(o)), 100); //$NON-NLS-1$
			FetchMembersOperation operation = new FetchMembersOperation(null, (ISVNRemoteFolder)o, collector);
			operation.run(Policy.subMonitorFor(monitor, 100));
        } catch (InvocationTargetException e) {
        	SVNUIPlugin.openError(null, null, null, e);
		} catch (InterruptedException e) {
			// Cancelled by the user;
		} finally {
            monitor.done();
        }
    }

    public ISchedulingRule getRule(Object element) {
    	ISVNRepositoryLocation location = getRepositoryLocation(element);
        return new RepositoryLocationSchedulingRule(location); //$NON-NLS-1$
    }

	protected ISVNRepositoryLocation getRepositoryLocation(Object o) {
		if (!(o instanceof ISVNRemoteFolder))
			return null;
		return ((ISVNRemoteFolder)o).getRepository();
	}

	public boolean isContainer() {
        return true;
    }

    /**
	 * @see org.tigris.subversion.subclipse.ui.model.SVNModelElement#isNeedsProgress()
	 */
	public boolean isNeedsProgress() {
		return true;
	}

    /**
     * Return null.
     */
    public Object getParent(Object o) {
        if (!(o instanceof ISVNRemoteFolder)) return null;
        ISVNRemoteFolder folder = (ISVNRemoteFolder)o;

        ISVNRemoteFolder parentFolder = folder.getParent();
        if (parentFolder != null)
            return parentFolder;
        else
        {
            return folder.getRepository();
        }
    }

}
