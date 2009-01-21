package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardMarkResolvedPage;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class ResolveActionWithChoices extends ResolveAction {
	private int selectedResolution;
	
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		boolean folderSelected = false;
		boolean propertyConflicts = false;
		boolean textConflicts = false;
		boolean treeConflicts = false;
		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IContainer) {
				folderSelected = true;
				break;
			}
			if (!propertyConflicts || !textConflicts || !treeConflicts) {
				ISVNLocalResource resource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
				try {
					LocalResourceStatus status = resource.getStatus();
					if (status != null && status.isPropConflicted()) propertyConflicts = true;
					if (status != null && status.isTextConflicted()) textConflicts = true;
					if (status != null && status.hasTreeConflict()) treeConflicts = true;
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}		
		}
		if (folderSelected) {
			selectedResolution = ISVNConflictResolver.Choice.chooseMerged;
			setResolution(selectedResolution);
		} else {
			if (propertyConflicts && !textConflicts) {
				String message;
				if (resources.length > 1) message = Policy.bind("ResolveAction.confirmMultiple"); //$NON-NLS-1$
				else message = Policy.bind("ResolveAction.confirm", resources[0].getName()); //$NON-NLS-1$
				if (!MessageDialog.openConfirm(getShell(), Policy.bind("ResolveOperation.taskName"), message)) return; //$NON-NLS-1$
				setResolution(ISVNConflictResolver.Choice.chooseMerged);				
			} else {
				SvnWizardMarkResolvedPage markResolvedPage = new SvnWizardMarkResolvedPage(resources);
				markResolvedPage.setPropertyConflicts(propertyConflicts);
				markResolvedPage.setTreeConflicts(treeConflicts);
				SvnWizard wizard = new SvnWizard(markResolvedPage);
		        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
		        wizard.setParentDialog(dialog);
		        if (dialog.open() == SvnWizardDialog.CANCEL) return;
		        setResolution(markResolvedPage.getResolution());
			}
		}
		super.execute(action);
	}

}
