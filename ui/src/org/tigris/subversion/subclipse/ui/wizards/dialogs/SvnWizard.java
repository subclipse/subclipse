package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.IClosableWizard;

public class SvnWizard extends Wizard implements IClosableWizard {
	private SvnWizardDialogPage svnWizardDialogPage;
	private Dialog parentDialog;

	public SvnWizard(SvnWizardDialogPage svnWizardDialogPage) {
		super();
		this.svnWizardDialogPage = svnWizardDialogPage;
	}

	public void addPages() {
		super.addPages();
		setWindowTitle(svnWizardDialogPage.getWindowTitle());
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
	
    public void setParentDialog(Dialog dialog) {
        this.parentDialog = dialog;
    } 
    
    public void finishAndClose() {
    	if (parentDialog != null && parentDialog instanceof ClosableWizardDialog && canFinish()) {
    		((ClosableWizardDialog)parentDialog).finishPressed();
    	}
    }	

}
