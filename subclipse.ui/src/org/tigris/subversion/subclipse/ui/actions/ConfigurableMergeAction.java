package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class ConfigurableMergeAction extends WorkbenchWindowAction {

    public ConfigurableMergeAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		super.setActivePart(action, targetPart);
		SVNUIPlugin.getDefaultMergeProvider().setActivePart(action, targetPart);
	}

	public void selectionChanged(IAction action, ISelection sel) {
		super.selectionChanged(action, sel);
		SVNUIPlugin.getDefaultMergeProvider().selectionChanged(action, sel);
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		SVNUIPlugin.getDefaultMergeProvider().execute(action);
    }

	protected String getErrorTitle() {
		return SVNUIPlugin.getDefaultMergeProvider().getErrorTitle();
	}

	protected boolean isEnabledForManagedResources() {
		return SVNUIPlugin.getDefaultMergeProvider().isEnabledForManagedResources();
	}

	protected boolean isEnabledForUnmanagedResources() {
		return SVNUIPlugin.getDefaultMergeProvider().isEnabledForUnmanagedResources();
	}

	protected boolean isEnabledForMultipleResources() {
		return SVNUIPlugin.getDefaultMergeProvider().isEnabledForMultipleResources();
	}	       

	protected String getImageId() {
		return SVNUIPlugin.getDefaultMergeProvider().getImageId();
	}	

}
