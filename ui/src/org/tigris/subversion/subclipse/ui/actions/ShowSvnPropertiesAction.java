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
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.svnproperties.SvnPropertiesView;


/**
 * 
 * Action for Show svn properties
 * 
 * 
 */
public class ShowSvnPropertiesAction extends WorkspaceAction {
	
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        IResource resource = (IResource)getSelectedResources()[0];
		final ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        try {        
		    SvnPropertiesView view = (SvnPropertiesView)showView(SvnPropertiesView.VIEW_ID);
		    if (view != null)
		        view.showSvnProperties(svnResource);
		} catch (SVNException e) {
            throw new InvocationTargetException(e);
		}

	}

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
     */
    protected boolean isEnabled() throws TeamException {
        return (selection.size() == 1);
    }

}
