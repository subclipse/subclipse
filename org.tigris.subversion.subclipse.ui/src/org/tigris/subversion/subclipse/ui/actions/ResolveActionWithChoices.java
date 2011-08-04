package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.ResolveTreeConflictWizard;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardMarkResolvedPage;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

public class ResolveActionWithChoices extends ResolveAction {
	private int selectedResolution;
	private SVNTreeConflict treeConflict;
	
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		boolean folderSelected = false;
		boolean propertyConflicts = false;
		boolean textConflicts = false;
		boolean treeConflicts = false;
		boolean treeConflictDialogShown = false;
		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IContainer) {
				folderSelected = true;
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
		if (resources.length == 1 && treeConflicts && !propertyConflicts && !textConflicts) {
			treeConflict = getTreeConflict(resources[0]);
			if (treeConflict != null) {
				ResolveTreeConflictWizard wizard = new ResolveTreeConflictWizard(treeConflict, getTargetPart());
				WizardDialog dialog = new SizePersistedWizardDialog(Display.getDefault().getActiveShell(), wizard, "ResolveTreeConflict"); //$NON-NLS-1$
				if (dialog.open() != WizardDialog.OK) return;
				treeConflictDialogShown = true;
			}			
		}
		if (resources.length > 1 && treeConflicts) {
			if (!MessageDialog.openConfirm(getShell(), Policy.bind("ResolveOperation.taskName"), Policy.bind("ResolveAction.confirmTreeConflicts"))) return; //$NON-NLS-1$	//$NON-NLS-2$		
			setResolution(ISVNConflictResolver.Choice.chooseMerged);				
		}
		else if (!treeConflictDialogShown) {
			SvnWizardMarkResolvedPage markResolvedPage = new SvnWizardMarkResolvedPage(resources);
			markResolvedPage.setPropertyConflicts(propertyConflicts);
			markResolvedPage.setTreeConflicts(treeConflicts);
			SvnWizard wizard = new SvnWizard(markResolvedPage);
	        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
	        wizard.setParentDialog(dialog);
	        if (dialog.open() == SvnWizardDialog.CANCEL) return;
	        setResolution(markResolvedPage.getResolution());
		}
		if (!treeConflictDialogShown) super.execute(action);
	}
	
	private SVNTreeConflict getTreeConflict(final IResource resource) {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				ISVNClientAdapter client = null;
				try {
					client = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getSVNClient();
					ISVNStatus[] statuses = client.getStatus(resource.getLocation().toFile(), true, true, true);
					for (int i = 0; i < statuses.length; i++) {
						if (statuses[i].hasTreeConflict()) {
							treeConflict = new SVNTreeConflict(statuses[i]);
							break;
						}
					}
				} catch (Exception e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				finally {
					SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().returnSVNClient(client);
				}
			}			
		});
		return treeConflict;
	}	

}
