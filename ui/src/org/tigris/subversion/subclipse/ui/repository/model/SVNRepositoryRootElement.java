/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.repository.model;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * RemoteRootElement is the model element for a repository that
 * appears in the repositories view
 */
public class SVNRepositoryRootElement extends SVNModelElement {
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
	
}
