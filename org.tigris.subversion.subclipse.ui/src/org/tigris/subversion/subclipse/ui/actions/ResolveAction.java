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
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ResolveOperation;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

/**
 * Action to mark conflicted file as resolved. 
 */
public class ResolveAction extends WorkbenchWindowAction {
	private int resolution = ISVNConflictResolver.Choice.chooseMerged;
	
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
	    new ResolveOperation(getTargetPart(), getSelectedResources(), resolution).run();
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("ResolveAction.error"); //$NON-NLS-1$
	}

    /**
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNResource)
     */
    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
        try {
        	if (!super.isEnabledForSVNResource(svnResource)) {
        		return false;
        	}
        	LocalResourceStatus status = svnResource.getStatusFromCache();
            return status.isTextConflicted() || status.isPropConflicted() || status.hasTreeConflict();
        } catch (SVNException e) {
            return false;
        }
    }

	protected boolean isEnabledForInaccessibleResources() {
		return true;
	}
	
	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_RESOLVE;
	}
	public void setResolution(int resolution) {
		this.resolution = resolution;
	}
	
}
