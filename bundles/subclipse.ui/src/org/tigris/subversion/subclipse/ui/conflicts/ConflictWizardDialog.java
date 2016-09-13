package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.ui.Messages;

public class ConflictWizardDialog extends WizardDialog {
	
	public boolean yesNo;

	public ConflictWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}
	
	public ConflictWizardDialog(Shell parentShell, IWizard newWizard, boolean yesNo) {
		this(parentShell, newWizard);
		this.yesNo = yesNo;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (yesNo) {
			Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
			if (cancelButton != null) cancelButton.setText(Messages.ConflictWizardDialog_0);
		}
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		String customLabel;
		if (id == IDialogConstants.FINISH_ID) {
			if (yesNo) customLabel = Messages.ConflictWizardDialog_1;
			else customLabel = Messages.ConflictWizardDialog_2;
		} else customLabel = label;
		return super.createButton(parent, id, customLabel, defaultButton);
	}

}
