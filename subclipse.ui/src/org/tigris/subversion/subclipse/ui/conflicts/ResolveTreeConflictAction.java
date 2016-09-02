package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;

public class ResolveTreeConflictAction extends Action {
	private ISelectionProvider selectionProvider;
	private IWorkbenchPart targetPart;

	public ResolveTreeConflictAction(ISelectionProvider selectionProvider, IWorkbenchPart targetPart) {
		super();
		this.selectionProvider = selectionProvider;
		this.targetPart = targetPart;
		setText(Policy.bind("ResolveTreeConflictAction.text")); //$NON-NLS-1$
	}
	
	public void run() {
		IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
		SVNTreeConflict treeConflict = (SVNTreeConflict)selection.getFirstElement();
		ResolveTreeConflictWizard wizard = new ResolveTreeConflictWizard(treeConflict, targetPart);
		WizardDialog dialog = new SizePersistedWizardDialog(Display.getDefault().getActiveShell(), wizard, "ResolveTreeConflict"); //$NON-NLS-1$
		dialog.open();
	}

}
