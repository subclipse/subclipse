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

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.operations.ShowAnnotationOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardAnnotatePage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;

public class AnnotateAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ISVNRemoteFile[] remoteFiles = getSelectedRemoteFiles();
		SvnWizardAnnotatePage annotatePage = new SvnWizardAnnotatePage(remoteFiles[0]);
		SvnWizard wizard = new SvnWizard(annotatePage);
        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
        wizard.setParentDialog(dialog);
        if (dialog.open() == SvnWizardDialog.CANCEL) return;
        ShowAnnotationOperation showAnnotationOperation = new ShowAnnotationOperation(getTargetPart(), remoteFiles[0], annotatePage.getFromRevision(), annotatePage.getToRevision(), annotatePage.isIncludeMergedRevisions(), annotatePage.isIgnoreMimeType());
        showAnnotationOperation.run();
//		AnnotateDialog dialog = new AnnotateDialog(getShell(), getTargetPart(), remoteFiles[0]);
//		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteFiles().length == 1;
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_ANNOTATE;
	}

}
