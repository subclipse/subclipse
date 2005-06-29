/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
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