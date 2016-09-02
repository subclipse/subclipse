package org.tigris.subversion.subclipse.graph.popup.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.actions.WorkbenchWindowAction;

public class ViewGraphAction extends WorkbenchWindowAction {
	private IResource selectedResource;

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
							ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);
							ISVNRemoteResource remoteResource = svnResource.getBaseResource();
							if (remoteResource != null) {
								String repoPath = remoteResource.getRepositoryRelativePath();
								if (repoPath == null || repoPath.length() == 0) {
									MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("ViewGraphAction.0"), Policy.bind("ViewGraphAction.1")); //$NON-NLS-1$ //$NON-NLS-2$
									return;
								}
							}
	//						IEditorPart part =
							getTargetPage().openEditor(
									new RevisionGraphEditorInput(resources[0]),
									"org.tigris.subversion.subclipse.graph.editors.revisionGraphEditor"); //$NON-NLS-1$
						}
					} catch (Exception e) {
						Activator.handleError(e);
						Activator.showErrorDialog(getErrorTitle(), e);
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

	protected IResource[] getSelectedResources() {
		if (selectedResource != null) {
			IResource[] selectedResources  = { selectedResource };
			return selectedResources;
		}
		return super.getSelectedResources();
	}
	
	public void setSelectedResource(IResource selectedResource) {
		this.selectedResource = selectedResource;
	}

	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

}