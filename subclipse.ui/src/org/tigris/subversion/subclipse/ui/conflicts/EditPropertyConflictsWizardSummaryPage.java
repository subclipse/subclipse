package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class EditPropertyConflictsWizardSummaryPage extends WizardPage {
	protected Button markResolvedButton;

	public EditPropertyConflictsWizardSummaryPage() {
		super("summary", Messages.EditPropertyConflictsWizardSummaryPage_1, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN)); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		EditPropertyConflictsWizard wizard = (EditPropertyConflictsWizard)getWizard();
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		outerContainer.setLayout(new GridLayout());
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Group summaryGroup = new Group(outerContainer, SWT.NULL);
		GridLayout summaryLayout = new GridLayout();
		summaryLayout.numColumns = 1;
		summaryGroup.setLayout(summaryLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		summaryGroup.setLayoutData(data);	
		summaryGroup.setText(Messages.EditPropertyConflictsWizardSummaryPage_2);
		
		Text summaryText = new Text(summaryGroup, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 100;
		data.widthHint = 400;
		data.grabExcessHorizontalSpace = true;
		summaryText.setLayoutData(data);	
		summaryText.setText(wizard.getConflictSummary());
		
		markResolvedButton = new Button(outerContainer, SWT.CHECK);
		markResolvedButton.setText(Messages.EditPropertyConflictsWizardSummaryPage_3);
		try {
			markResolvedButton.setVisible(!wizard.getSvnResource().getStatus().isTextConflicted());
		} catch (SVNException e) {}
		
		setControl(outerContainer);
		
		setMessage(wizard.getSvnResource().getName() + Messages.EditPropertyConflictsWizardSummaryPage_4);
	}

}
