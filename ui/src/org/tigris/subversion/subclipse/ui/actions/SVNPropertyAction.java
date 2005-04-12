/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

/**
 * ancestor to all actions that work on ISVNProperty objects
 */
abstract public class SVNPropertyAction extends SVNAction {

	/**
	 * return the ISVNLocalResource associated to this property or null
	 * @param svnProperty
	 * @return
	 */
	protected ISVNLocalResource getSVNLocalResource(ISVNProperty svnProperty) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		
		File file = svnProperty.getFile();
		if (!file.exists()) {
			return null;
		}
		
		IPath pathEclipse; 
		pathEclipse = new Path(file.getAbsolutePath());

		IResource resource;
		if (file.isDirectory()) {
			resource = workspaceRoot.getContainerForLocation(pathEclipse);
		} else {
			resource =  workspaceRoot.getFileForLocation(pathEclipse);
		}
		if (resource == null) {
			return null;
		}
		return SVNWorkspaceRoot.getSVNResourceFor(resource);
	}

	/**
	 * Returns the selected svn properties
	 */
	protected ISVNProperty[] getSelectedSvnProperties() {
		ArrayList resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = ((IStructuredSelection)selection).iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof ISVNProperty) {
					resources.add(next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(ISVNProperty.class);
					if (adapter instanceof ISVNProperty) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ISVNProperty[] result = new ISVNProperty[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ISVNProperty[0];
	}

}
