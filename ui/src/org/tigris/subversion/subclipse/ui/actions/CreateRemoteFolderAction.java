/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.NewRemoteFolderWizard;

/**
 * Action to create a remote folder on repository
 */
public class CreateRemoteFolderAction extends SVNAction {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action){
          
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
        
        WizardDialog dialog = new ClosableWizardDialog(shell, wizard);
        wizard.setParentDialog(dialog);
        dialog.open();

	}

	protected boolean isEnabled(){
		return (selection.size() == 1);
	}

}
