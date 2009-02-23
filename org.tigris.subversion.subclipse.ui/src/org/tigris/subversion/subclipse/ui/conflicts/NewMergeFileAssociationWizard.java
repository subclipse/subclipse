package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.tigris.subversion.subclipse.ui.Policy;

public class NewMergeFileAssociationWizard extends Wizard {
	private MergeFileAssociation[] mergeFileAssociations;
	private NewMergeFileAssociationWizardPage mainPage;
	private MergeFileAssociation mergeFileAssociation;

	public NewMergeFileAssociationWizard(MergeFileAssociation[] mergeFileAssociations) {
		this.mergeFileAssociations = mergeFileAssociations;
		setWindowTitle(TeamUIMessages.TextPreferencePage_6);
	}

	public void addPages() {
		mainPage = new NewMergeFileAssociationWizardPage("mainPage", Policy.bind("NewMergeFileAssociationWizard.heading"), null, mergeFileAssociations); //$NON-NLS-1$ //$NON-NLS-2$
		mainPage.setDescription(Policy.bind("NewMergeFileAssociationWizard.description")); //$NON-NLS-1$		
		addPage(mainPage);		
	}

	public boolean performFinish() {
		mergeFileAssociation = new MergeFileAssociation();
		mergeFileAssociation.setFileType(mainPage.fileTypeText.getText().trim());
		if (mainPage.builtInMergeRadioButton.getSelection()) mergeFileAssociation.setType(MergeFileAssociation.BUILT_IN);
		else if (mainPage.externalMergeRadioButton.getSelection()) mergeFileAssociation.setType(MergeFileAssociation.DEFAULT_EXTERNAL);
		else if (mainPage.customMergeRadioButton.getSelection()) {
			mergeFileAssociation.setType(MergeFileAssociation.CUSTOM_EXTERNAL);
			mergeFileAssociation.setMergeProgram(mainPage.customProgramLocationCombo.getText().trim());
			mergeFileAssociation.setParameters(mainPage.customProgramParametersText.getText().trim());
		}
		return true;
	}

	public MergeFileAssociation getMergeFileAssociation() {
		return mergeFileAssociation;
	}

}
