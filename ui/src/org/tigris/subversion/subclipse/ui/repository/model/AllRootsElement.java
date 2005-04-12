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

 
import java.util.Arrays;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.repo.RepositoryComparator;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * AllRootsElement is the model element for the repositories view.
 * Its children are the array of all known repository roots.
 * 
 * Because we extend IAdaptable, we don't need to register this adapter
 * as we need for RemoteFileElement, RemoteFolderElement ...
 */
public class AllRootsElement extends SVNModelElement  implements IAdaptable  {
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
        ISVNRepositoryLocation[] locations =
            SVNUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
        Arrays.sort(locations, new RepositoryComparator());
        return locations; 	
    }
	public String getLabel(Object o) {
		return null;
	}
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	public Object getParent(Object o) {
		return null;
	}
}

