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

import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.CleanupOperation;

/**
 * Action to recursively cleanup any locks in teh working copy
 */
public class CleanupAction extends WorkbenchWindowAction {

    protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
        new CleanupOperation(getTargetPart(), getSelectedResources()).run();
    }

    /**
     * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
     */
    protected String getErrorTitle() {
        return Policy.bind("CleanupAction.error"); //$NON-NLS-1$
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

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_CLEANUP;
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
}
