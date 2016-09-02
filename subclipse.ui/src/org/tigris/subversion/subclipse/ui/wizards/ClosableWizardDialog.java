package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class ClosableWizardDialog extends WizardDialog {

	public ClosableWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}

	public void finishPressed() {
		super.finishPressed();
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

}
