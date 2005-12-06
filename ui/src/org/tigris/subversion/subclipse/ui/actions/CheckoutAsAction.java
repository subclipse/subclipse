/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.ui.WorkspacePathValidator;
import org.tigris.subversion.subclipse.ui.wizards.CheckoutWizard;

/**
 * Add a remote resource to the workspace. Current implementation:
 * - Works only for remote folders
 * - prompt for project name
 */
public class CheckoutAsAction extends SVNAction {

    /*
     * @see TeamAction#isEnabled()
     */
    protected boolean isEnabled() {
//        return getSelectedRemoteFolders().length == 1;
    	return getSelectedRemoteFolders().length > 0;
    }

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		if (!WorkspacePathValidator.validateWorkspacePath()) return;
	    final ISVNRemoteFolder[] folders = getSelectedRemoteFolders();
	    
	    CheckoutWizard wizard = new CheckoutWizard(folders);
	    WizardDialog dialog = new WizardDialog(shell, wizard);
	    dialog.open();
	}

}
