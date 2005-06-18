package org.tigris.subversion.subclipse.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class WorkspacePathValidator {

    public static boolean validateWorkspacePath() {
        File file = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        String canonicalPath = null;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!file.getAbsolutePath().equals(canonicalPath)) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), Policy.bind("WorkspacePathValidator.title"),
            Policy.bind("WorkspacePathValidator.eclipsePath") + "\n\n"  + file.getAbsolutePath() + 
            "\n\n" + Policy.bind("WorkspacePathValidator.fileSystemPath") + "\n\n" + canonicalPath +
            "\n\n" + Policy.bind("WorkspacePathValidator.instructions"));
            return false;
        }
        return true;
    }

}
