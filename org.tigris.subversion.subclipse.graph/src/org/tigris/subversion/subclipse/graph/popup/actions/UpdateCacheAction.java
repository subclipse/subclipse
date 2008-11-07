package org.tigris.subversion.subclipse.graph.popup.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.graph.editors.GraphBackgroundTask;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;

public class UpdateCacheAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final ISVNRemoteFolder[] selectedFolders = getSelectedRemoteFolders();
		GraphBackgroundTask task =
			new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), null, null, selectedFolders[0]);
		try {
			task.run();
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("UpdateCacheAction.title"), e.getMessage()); //$NON-NLS-1$
		}	
	}

	protected boolean isEnabled() throws TeamException {
		return true;
	}

}
