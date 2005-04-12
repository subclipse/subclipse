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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.svnproperties.SetSvnPropertyDialog;

/**
 * Set a new svn property on a given resource 
 */
public class SetSvnPropertyAction extends WorkspaceAction {
	
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				IResource resource = getSelectedResources()[0];
				ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				SetSvnPropertyDialog dialog = new SetSvnPropertyDialog(getShell(),svnResource);
				if (dialog.open() != SetSvnPropertyDialog.OK) return;
			
				try {
					if (dialog.getPropertyValue() != null) {
						svnResource.setSvnProperty(dialog.getPropertyName(), dialog.getPropertyValue(),dialog.getRecurse());
					} else {
						svnResource.setSvnProperty(dialog.getPropertyName(), dialog.getPropertyFile(),dialog.getRecurse());
					}
				
				} catch (SVNException e) {
					throw new InvocationTargetException(e);
				}
			} 
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("SetSvnPropertyAction.set"); //$NON-NLS-1$
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
	
}
