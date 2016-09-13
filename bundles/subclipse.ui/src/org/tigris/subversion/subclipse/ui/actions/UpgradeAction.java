package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.UpgradeOperation;

public class UpgradeAction extends WorkbenchWindowAction {

    protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
    	String message;
    	IResource[] resources = getSelectedResources();
    	if (resources.length == 1) {
    		message = Policy.bind("UpgradeAction.confirm.single", resources[0].getName()); //$NON-NLS-1$
    	}
    	else {
    		message = Policy.bind("UpgradeAction.confirm.multiple"); //$NON-NLS-1$
    	}
    	if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Policy.bind("UpgradeAction.title"), message)) { //$NON-NLS-1$
    		return;
    	}
        new UpgradeOperation(getTargetPart(), getSelectedResources()).run();
    }	
    
    /**
     * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
     */
    protected String getErrorTitle() {
        return Policy.bind("UpgradeAction.error"); //$NON-NLS-1$
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNLocalResource)
     */
    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) throws SVNException {
        return svnResource.isFolder() && super.isEnabledForSVNResource(svnResource);
    }
    
    /**
     * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
     */
    protected boolean isEnabledForAddedResources() {
        return false;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
}
