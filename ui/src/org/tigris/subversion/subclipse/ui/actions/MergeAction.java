package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.MergeDialog;
import org.tigris.subversion.subclipse.ui.operations.MergeOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeAction extends WorkspaceAction {

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        IResource[] resources = getSelectedResources(); 
        for (int i = 0; i < resources.length; i++) {
            MergeDialog dialog = new MergeDialog(getShell(), resources[i]);
            if (dialog.open() == MergeDialog.CANCEL) break;
            SVNUrl svnUrl1 = dialog.getFromUrl();
            SVNRevision svnRevision1 = dialog.getFromRevision();
            SVNUrl svnUrl2 = dialog.getToUrl();
            SVNRevision svnRevision2 = dialog.getToRevision();            
            new MergeOperation(getTargetPart(), getSelectedResources(), svnUrl1, svnRevision1, svnUrl2, svnRevision2).run();      
        }   
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("MergeAction.merge"); //$NON-NLS-1$
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

}
