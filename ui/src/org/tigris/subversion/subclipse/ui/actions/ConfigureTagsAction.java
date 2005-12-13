package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.ui.dialogs.ConfigureTagsDialog;

public class ConfigureTagsAction extends WorkspaceAction {

	public ConfigureTagsAction() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
		ConfigureTagsDialog dialog = new ConfigureTagsDialog(getShell(), resources[0]);
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedResources().length == 1;
	}

}
