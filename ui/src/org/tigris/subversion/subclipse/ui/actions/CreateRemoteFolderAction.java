/*
 * Created on 28 juin 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.wizards.NewRemoteFolderWizard;

/**
 * @author cedric
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CreateRemoteFolderAction extends SVNAction {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action)throws InvocationTargetException, InterruptedException {
          
        ISVNRemoteFolder remoteFolder = null;
                 
        if (selection.getFirstElement() instanceof ISVNRemoteFolder)
            remoteFolder = (ISVNRemoteFolder)selection.getFirstElement();
        else
        if (selection.getFirstElement() instanceof ISVNRepositoryLocation)
            remoteFolder = ((ISVNRepositoryLocation)selection.getFirstElement()).getRootFolder();
        else
        if (selection.getFirstElement() instanceof ISVNRemoteFile)
            remoteFolder = ((ISVNRemoteFile)selection.getFirstElement()).getParent();
                
        NewRemoteFolderWizard wizard = new NewRemoteFolderWizard(remoteFolder);
                
        WizardDialog dialog = new WizardDialog(shell, wizard);
        wizard.setParentDialog(dialog);
        dialog.open();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return (selection.size() == 1);
	}

}
