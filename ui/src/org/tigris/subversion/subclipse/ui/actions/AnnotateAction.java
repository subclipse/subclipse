package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.dialogs.AnnotateDialog;

public class AnnotateAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ISVNRemoteFile[] remoteFiles = getSelectedRemoteFiles();
		AnnotateDialog dialog = new AnnotateDialog(getShell(), getTargetPart(), remoteFiles[0]);
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteFiles().length == 1;
	}

}
