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
import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.repo.SVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.RelocateWizard;

public class RelocateAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof SVNRepositoryLocation) {
				SVNRepositoryLocation repository = (SVNRepositoryLocation)object;
				RelocateWizard wizard = new RelocateWizard(repository);
				WizardDialog dialog = new ClosableWizardDialog(shell, wizard);
				dialog.open();
				break;
			}
		}
	}

	protected boolean isEnabled() throws TeamException {
		return selection.size() == 1;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_RELOCATE;
	}

}
