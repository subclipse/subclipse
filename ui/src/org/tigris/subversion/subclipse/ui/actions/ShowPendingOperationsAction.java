/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.pending.PendingOperationsView;


/**
 * 
 * Action for Show Pending operations
 * 
 * 
 */
public class ShowPendingOperationsAction extends WorkspaceAction {
	
	protected void execute(IAction action) throws InvocationTargetException {
        final IContainer container = (IContainer)getSelectedResources()[0];
        
        try {        
		    PendingOperationsView view = (PendingOperationsView)showView(PendingOperationsView.VIEW_ID);
		    if (view != null)
		        view.showPending(container);
		} catch (SVNException e) {
            throw new InvocationTargetException(e);
		}

	}

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
     */
    protected boolean isEnabled() {
        return (selection.size() == 1);
    }

}
