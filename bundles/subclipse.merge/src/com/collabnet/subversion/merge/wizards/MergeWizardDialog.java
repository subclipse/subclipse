/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.collabnet.subversion.merge.Messages;

public class MergeWizardDialog extends WizardDialog {
	
	public boolean yesNo;

	public MergeWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}
	
	public MergeWizardDialog(Shell parentShell, IWizard newWizard, boolean yesNo) {
		this(parentShell, newWizard);
		this.yesNo = yesNo;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (yesNo) {
			Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
			if (cancelButton != null) cancelButton.setText(Messages.MergeWizardDialog_no);
		}
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		String customLabel;
		if (id == IDialogConstants.FINISH_ID) {
			if (yesNo) customLabel = Messages.MergeWizardDialog_yes;
			else customLabel = Messages.MergeWizardDialog_ok;
		} else customLabel = label;
		return super.createButton(parent, id, customLabel, defaultButton);
	}

}
