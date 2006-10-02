/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.SwitchDialog;
import org.tigris.subversion.subclipse.ui.operations.SwitchOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Action to switch to branch/tag 
 */
public class SwitchAction extends WorkspaceAction {

    protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        IResource[] resources = getSelectedResources(); 
        for (int i = 0; i < resources.length; i++) {
            SwitchDialog dialog = new SwitchDialog(getShell(), resources[i]);
            if (dialog.open() == SwitchDialog.CANCEL) break;
            SVNUrl svnUrl = dialog.getUrl();
            SVNRevision svnRevision = dialog.getRevision();
            new SwitchOperation(getTargetPart(), getSelectedResources(), svnUrl, svnRevision).run();
        }
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("SwitchAction.switch"); //$NON-NLS-1$
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

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_SWITCH;
	}

}
