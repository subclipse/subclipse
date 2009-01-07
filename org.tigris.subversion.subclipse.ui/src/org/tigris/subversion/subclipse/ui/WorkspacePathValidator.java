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

import org.eclipse.core.runtime.IStatus;

//import java.io.File;
//import java.io.IOException;
//
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.swt.widgets.Display;

public class WorkspacePathValidator {

    public static boolean validateWorkspacePath() {
    	return true;
//        File file = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
//        String canonicalPath = null;
//        try {
//            canonicalPath = file.getCanonicalPath();
//        } catch (IOException e) {
//            SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
//        }
//        if (!file.getAbsolutePath().equals(canonicalPath)) {
//            MessageDialog.openError(Display.getCurrent().getActiveShell(), Policy.bind("WorkspacePathValidator.title"),
//            Policy.bind("WorkspacePathValidator.eclipsePath") + "\n\n"  + file.getAbsolutePath() + 
//            "\n\n" + Policy.bind("WorkspacePathValidator.fileSystemPath") + "\n\n" + canonicalPath +
//            "\n\n" + Policy.bind("WorkspacePathValidator.instructions"));
//            return false;
//        }
//        return true;
    }

}
