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
package org.tigris.subversion.subclipse.core.util;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Utility class to get the IResource corresponding to given File
 * 
 * @author Cedric Chabanois (cchab at tigris.org)
 */
public class File2Resource {

    /**
     * get the IResource corresponding to the given file. Given file should
     * exist because it is used to determine whether we need to create a
     * IContainer or a IFile
     * 
     * @param file
     * @return
     */
    public static IResource getResource(File file) {
    	if (file == null) return null;
        return getResource(file, file.isDirectory());
    }

    /**
     * get the IResource corresponding to the given file. Given file does not
     * need to exist.
     * 
     * @param file
     * @param isDirectory
     *            if true, an IContainer will be returned, otherwise an IFile
     *            will be returned
     * @return
     */
    public static IResource getResource(File file, boolean isDirectory) {
    	if (file == null) return null;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();

        IPath pathEclipse = new Path(file.getAbsolutePath());

        IResource resource = null;

        if (isDirectory) {
            resource = workspaceRoot.getContainerForLocation(pathEclipse);
        } else {
            resource = workspaceRoot.getFileForLocation(pathEclipse);
        }
        return resource;
    }

}