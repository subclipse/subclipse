package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardMarkResolvedPage;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class ResolveActionWithChoices extends ResolveAction {
	private int selectedResolution;
	
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		boolean folderSelected = false;
		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IContainer) {
				folderSelected = true;
				break;
			}
		}
		if (folderSelected) {
			selectedResolution = ISVNConflictResolver.Choice.chooseMerged;
			setResolution(selectedResolution);
		} else {
			SvnWizardMarkResolvedPage markResolvedPage = new SvnWizardMarkResolvedPage(resources);
			SvnWizard wizard = new SvnWizard(markResolvedPage);
	        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
	        wizard.setParentDialog(dialog);
	        if (dialog.open() == SvnWizardDialog.CANCEL) return;
	        setResolution(markResolvedPage.getResolution());
		}
		super.execute(action);
	}

}
