package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.BranchTagDialog;
import org.tigris.subversion.subclipse.ui.operations.BranchTagOperation;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagAction extends WorkspaceAction {

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        IResource[] resources = getSelectedResources();
        for (int i = 0; i < resources.length; i++) {
            BranchTagDialog dialog = new BranchTagDialog(getShell(), resources[i]);
            if (dialog.open() == BranchTagDialog.CANCEL) break;
            SVNUrl sourceUrl = dialog.getUrl();
            SVNUrl destinationUrl = dialog.getToUrl();
            String message = dialog.getComment();
            boolean createOnServer = dialog.isCreateOnServer();
            new BranchTagOperation(getTargetPart(), getSelectedResources(), sourceUrl, destinationUrl, createOnServer, dialog.getRevision(), message).run();
        }          
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("BranchTagAction.branch"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForManagedResources()
	 */
	protected boolean isEnabledForManagedResources() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
		return false;
	}	       
    

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
    protected boolean isEnabledForAddedResources() {
        return false;
    }
}
