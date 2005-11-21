package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.ui.dialogs.ExportRemoteFolderDialog;

public class ExportRemoteFolderAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ISVNRemoteFolder[] folders = getSelectedRemoteFolders();
		ExportRemoteFolderDialog dialog = new ExportRemoteFolderDialog(getShell(), folders[0]);
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteResources().length == 1;
	}

}
