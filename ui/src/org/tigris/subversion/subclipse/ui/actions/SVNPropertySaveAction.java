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

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

/**
 * action to save a property
 */
public class SVNPropertySaveAction extends SVNPropertyAction {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action)
		throws InvocationTargetException, InterruptedException {
		ISVNProperty svnProperty = getSelectedSvnProperties()[0];

		SaveAsDialog dialog = new SaveAsDialog(getShell());

		if (dialog.open() != SaveAsDialog.OK)
			return;

		IFile file =
			ResourcesPlugin.getWorkspace().getRoot().getFile(
				dialog.getResult());
		try {
			ByteArrayInputStream is =
				new ByteArrayInputStream(svnProperty.getData());
			file.create(is, true, null);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return getSelectedSvnProperties().length == 1;
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("SVNPropertySaveAction.save"); //$NON-NLS-1$
	}

}
