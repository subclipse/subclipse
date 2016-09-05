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
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.svnproperties.SvnPropertiesView;


/**
 * 
 * Action for Show svn properties
 * 
 * 
 */
public class ShowSvnPropertiesAction extends WorkbenchWindowAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
        	if (getSelectedResources() != null && getSelectedResources().length > 0) {
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
        }
	}

	protected boolean isEnabledForAddedResources() {
		return true;
	}

	protected boolean isEnabledForIgnoredResources() {
		return false;
	}

	protected boolean isEnabledForInaccessibleResources() {
		return false;
	}

	protected boolean isEnabledForManagedResources() {
		return true;
	}

	protected boolean isEnabledForMultipleResources() {
		return false;
	}

	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_SHOWPROPERTY;
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
}
