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
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.FetchMembersOperation;

/**
 * RemoteRootElement is the model element for a repository that
 * appears in the repositories view
 */
public class SVNRepositoryRootElement extends SVNModelElement implements IDeferredWorkbenchAdapter {
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof ISVNRepositoryLocation /*|| object instanceof RepositoryRoot*/) {
			return SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REPOSITORY);
		}
		return null;
	}
	public String getLabel(Object o) {
		if (o instanceof ISVNRepositoryLocation) {
			ISVNRepositoryLocation root = (ISVNRepositoryLocation)o;
			return root.toString();
		}
		return null;
	}

	public Object getParent(Object o) {
		return null;
	}

	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
		ISVNRepositoryLocation location = null;
		if (o instanceof ISVNRepositoryLocation) {
			location = (ISVNRepositoryLocation)o;
		}
		if (location == null) return null;

		Object[] result = null;
		try {
			result = location.members(monitor);
		} catch (Exception e) {}
		return result;
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		// If it's not a folder, return an empty array
		if (!(o instanceof ISVNRepositoryLocation)) {
			collector.add(new Object[0], monitor);
		}
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(Policy.bind("RemoteFolderElement_fetchingRemoteMembers.message", getLabel(o)), 100); //$NON-NLS-1$
			FetchMembersOperation operation = new FetchMembersOperation(null, ((ISVNRepositoryLocation)o).getRootFolder(), collector);
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
		if (!(o instanceof ISVNRepositoryLocation))
			return null;
		return (ISVNRepositoryLocation)o;
	}

	public boolean isContainer() {
		return true;
	}

}
