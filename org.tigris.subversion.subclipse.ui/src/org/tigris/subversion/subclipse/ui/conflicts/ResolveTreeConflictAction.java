package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.ui.Policy;

public class ResolveTreeConflictAction extends Action {
	private ISelectionProvider selectionProvider;

	public ResolveTreeConflictAction(ISelectionProvider selectionProvider) {
		super();
		this.selectionProvider = selectionProvider;
		setText(Policy.bind("ResolveTreeConflictAction.text")); //$NON-NLS-1$
	}
	
	public void run() {
		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Resolve Tree Conflict", "Not yet implemented.");
	}

}
