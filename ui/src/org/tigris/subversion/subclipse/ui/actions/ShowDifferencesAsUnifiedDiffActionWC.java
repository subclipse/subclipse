package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.ui.dialogs.ShowDifferencesAsUnifiedDiffDialogWC;

public class ShowDifferencesAsUnifiedDiffActionWC extends WorkspaceAction {

	public ShowDifferencesAsUnifiedDiffActionWC() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
		ShowDifferencesAsUnifiedDiffDialogWC dialog = new ShowDifferencesAsUnifiedDiffDialogWC(getShell(), resources[0], getTargetPart());
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedResources().length == 1;
	}

}
