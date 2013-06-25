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
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.svnproperties.SvnPropertiesView;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardSetPropertyPage;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

/**
 * action to modify a property
 */
public class SVNPropertyModifyAction extends SVNPropertyAction {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action)
		throws InvocationTargetException, InterruptedException {
			run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
					ISVNProperty svnProperty = getSelectedSvnProperties()[0];
					ISVNLocalResource svnResource = getSVNLocalResource(svnProperty);
					SvnWizardSetPropertyPage setPropertyPage = new SvnWizardSetPropertyPage(svnResource, svnProperty);
					SvnWizard wizard = new SvnWizard(setPropertyPage);
				    SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
				    wizard.setParentDialog(dialog); 					
					if (dialog.open() != SvnWizardDialog.OK) return;
			
					try {
						if (setPropertyPage.getPropertyValue() != null) {
							svnResource.setSvnProperty(setPropertyPage.getPropertyName(), setPropertyPage.getPropertyValue(),setPropertyPage.getRecurse());
						} else {
							svnResource.setSvnProperty(setPropertyPage.getPropertyName(), setPropertyPage.getPropertyFile(),setPropertyPage.getRecurse());
						}
						SvnPropertiesView.refreshView();
					} catch (SVNException e) {
						throw new InvocationTargetException(e);
					}
				} 
			}, false /* cancelable */, PROGRESS_BUSYCURSOR);
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
		return Policy.bind("SVNPropertyModifyAction.modify"); //$NON-NLS-1$
	}

}
