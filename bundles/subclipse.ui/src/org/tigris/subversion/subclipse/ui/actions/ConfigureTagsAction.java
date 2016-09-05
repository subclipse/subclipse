/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardConfigureTagsPage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class ConfigureTagsAction extends WorkspaceAction {

	public ConfigureTagsAction() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
		ISVNLocalResource[] svnResources = new ISVNLocalResource[resources.length];
		ISVNProperty lastProperty = null;
		for (int i = 0; i < resources.length; i++) {
			svnResources[i] = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
			try {
				ISVNProperty property = svnResources[i].getSvnProperty("subclipse:tags"); //$NON-NLS-1$
				if (i > 0 && !propertiesEqual(property, lastProperty)) {
					MessageDialog.openError(getShell(), Messages.ConfigureTagsAction_1, Messages.ConfigureTagsAction_2);
					return;
				}
				lastProperty = property;
			} catch (SVNException e) {}
		}			
		SvnWizardConfigureTagsPage configureTagsPage = new SvnWizardConfigureTagsPage(svnResources);
		SvnWizard wizard = new SvnWizard(configureTagsPage);
        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
        wizard.setParentDialog(dialog);  		
		dialog.open();
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	private boolean propertiesEqual(ISVNProperty property1, ISVNProperty property2) {
		String value1 = null;
		String value2 = null;
		if (property1 != null) value1 = property1.getValue();
		if (property2 != null) value2 = property2.getValue();
		if (value1 == null) value1 = ""; //$NON-NLS-1$
		if (value2 == null) value2 = ""; //$NON-NLS-1$
		return value1.equals(value2);
	}

}
