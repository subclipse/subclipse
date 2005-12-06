package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.subclipse.ui.Policy;

public class CheckoutWizardCheckoutAsMultiplePage extends WizardPage {
	private Label textLabel;
	private Button projectsButton;
	private Button existingButton;

	public CheckoutWizardCheckoutAsMultiplePage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	public void createControl(Composite parent) {	
		CheckoutWizard wizard = (CheckoutWizard)getWizard();
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		textLabel = new Label(outerContainer, SWT.NONE);
		GridData data = new GridData();
		data.widthHint = 300;
		textLabel.setLayoutData(data);
		
		if (wizard.getRemoteFolders() != null) {
			textLabel.setText(Policy.bind("CheckoutWizardCheckoutAsPage.multiple", Integer.toString(wizard.getRemoteFolders().length))); //$NON-NLS-1$
		}
		
		projectsButton = new Button(outerContainer, SWT.RADIO);
		projectsButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.projects")); //$NON-NLS-1$
		
		existingButton = new Button(outerContainer, SWT.RADIO);
		existingButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.existing")); //$NON-NLS-1$
		existingButton.setEnabled(false);
		existingButton.setVisible(false);
		
		projectsButton.setSelection(true);

		setMessage(Policy.bind("CheckoutWizardCheckoutAsPage.text")); //$NON-NLS-1$
		
		setControl(outerContainer);	
	}
	
	public void setText(String text) {
		textLabel.setText(text);
	}

}
