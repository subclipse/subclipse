package org.tigris.subversion.subclipse.graph.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.ui.actions.ActionDelegate;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

public class SynchronizeViewGraphAction extends ActionDelegate {
	private IStructuredSelection fSelection;

	public SynchronizeViewGraphAction() {
		super();
	}

	public void run(IAction action) {
		ISynchronizeModelElement element = (ISynchronizeModelElement)fSelection.getFirstElement();
		IResource resource = element.getResource();
		ViewGraphAction viewGraphAction = new ViewGraphAction();
		viewGraphAction.setSelectedResource(resource);
		viewGraphAction.run(null);
	}

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
			if (action != null) {
				action.setEnabled(isEnabledForSelection());
			}
		}
	}

	private boolean isEnabledForSelection() {
		if (fSelection == null || fSelection.isEmpty()) return false;
		ISynchronizeModelElement element = (ISynchronizeModelElement)fSelection.getFirstElement();
		IResource resource = element.getResource();
		if (resource == null) return false;
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);	
		try {
			return (svnResource != null && svnResource.isManaged() && !svnResource.isAdded());
		} catch (SVNException e) {
			return false;
		}
	}

}
