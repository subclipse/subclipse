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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.DeletePropertyDialog;
import org.tigris.subversion.subclipse.ui.svnproperties.SvnPropertiesView;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

/**
 * action to modify a property
 */
public class SVNPropertyDeleteAction extends SVNPropertyAction {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action)
		throws InvocationTargetException, InterruptedException {
		
			run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
					ISVNProperty[] svnProperties = getSelectedSvnProperties();
					
					boolean directory = svnProperties[0].getFile().isDirectory();

					String message;
					if (svnProperties.length == 1) {
						message = Policy.bind("SVNPropertyDeleteAction.confirmSingle",svnProperties[0].getName()); //$NON-NLS-1$
					} else {
						message = Policy.bind("SVNPropertyDeleteAction.confirmMultiple",Integer.toString(svnProperties.length)); //$NON-NLS-1$
					}

					DeletePropertyDialog dialog = new DeletePropertyDialog(getShell(), message, directory);
					if (dialog.open() == DeletePropertyDialog.CANCEL) return;
					
					for (int i = 0; i < svnProperties.length;i++) {
						ISVNProperty svnProperty = svnProperties[i];  
						ISVNLocalResource svnResource = getSVNLocalResource(svnProperty);
						try {
							svnResource.deleteSvnProperty(svnProperty.getName(),dialog.isRecurse());
							SvnPropertiesView.refreshView();
						} catch (SVNException e) {
							throw new InvocationTargetException(e);
						}
						
					}
				} 
			}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return getSelectedSvnProperties().length > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("SVNPropertyDeleteAction.delete"); //$NON-NLS-1$
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_PROPDELETE;
	}

}
