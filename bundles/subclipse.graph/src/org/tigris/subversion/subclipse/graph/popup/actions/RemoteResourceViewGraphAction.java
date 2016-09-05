package org.tigris.subversion.subclipse.graph.popup.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;

public class RemoteResourceViewGraphAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				ISVNRemoteResource[] resources = getSelectedRemoteResources();
				if (resources.length == 0)
					resources = getSelectedRemoteFolders();
				try {
					if (resources.length > 0) {
						getTargetPage().openEditor(
								new RevisionGraphEditorInput(resources[0]),
								"org.tigris.subversion.subclipse.graph.editors.revisionGraphEditor");
					}
				} catch (Exception e) {
					Activator.handleError(e);
					Activator.showErrorDialog(getErrorTitle(), e);
				}				
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}
	
	protected String getErrorTitle() {
		return Policy.bind("ViewGraphAction.viewGraph"); //$NON-NLS-1$
	}

	protected boolean isEnabled() throws TeamException {
		ISVNRemoteResource[] resources = getSelectedRemoteResources();
		if (resources.length == 1) return true;
		if (resources.length == 0 && getSelectedRemoteFolders().length == 1) return true;
		return false;
	}

}
