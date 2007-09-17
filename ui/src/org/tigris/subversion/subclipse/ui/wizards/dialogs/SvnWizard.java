package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import org.eclipse.jface.wizard.Wizard;

public class SvnWizard extends Wizard {
	private SvnWizardDialogPage svnWizardDialogPage;

	public SvnWizard(SvnWizardDialogPage svnWizardDialogPage) {
		super();
		this.svnWizardDialogPage = svnWizardDialogPage;
	}

	public void addPages() {
		super.addPages();
		setWindowTitle(svnWizardDialogPage.getTitle());
		addPage(svnWizardDialogPage);
	}

	public boolean performFinish() {
		return svnWizardDialogPage.performFinish();
	}

	public boolean performCancel() {
		return svnWizardDialogPage.performCancel();
	}

	public SvnWizardDialogPage getSvnWizardDialogPage() {
		return svnWizardDialogPage;
	}

}
