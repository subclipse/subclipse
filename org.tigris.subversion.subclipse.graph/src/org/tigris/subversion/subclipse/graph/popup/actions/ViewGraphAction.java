package org.tigris.subversion.subclipse.graph.popup.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.actions.WorkbenchWindowAction;

public class ViewGraphAction extends WorkbenchWindowAction {

	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {		
			run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					IResource[] resources = getSelectedResources();
					try {
						if (resources.length > 0) {
	//						IEditorPart part =
							getTargetPage().openEditor(
									new RevisionGraphEditorInput(resources[0]),
									"org.tigris.subversion.subclipse.graph.editors.revisionGraphEditor");
						}
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, false /* cancelable */, PROGRESS_BUSYCURSOR);
        }
	}

	protected String getErrorTitle() {
		return Policy.bind("ViewGraphAction.viewGraph"); //$NON-NLS-1$
	}
	
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		return false;
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForCopiedResources()
	 */
	protected boolean isEnabledForCopiedResources() {
		return true;
	}

}