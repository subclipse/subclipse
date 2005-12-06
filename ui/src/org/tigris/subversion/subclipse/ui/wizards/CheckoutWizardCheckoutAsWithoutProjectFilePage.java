package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.ui.Policy;

public class CheckoutWizardCheckoutAsWithoutProjectFilePage extends WizardPage {
	private Label textLabel;
	private Button wizardButton;
	private Button projectButton;
	private Text projectText;
	private Button existingButton;

	public CheckoutWizardCheckoutAsWithoutProjectFilePage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		CheckoutWizard wizard = (CheckoutWizard)getWizard();
		ISVNRemoteFolder[] remoteFolders = wizard.getRemoteFolders();
		
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
		
		if (remoteFolders != null) {
			textLabel.setText(Policy.bind("CheckoutWizardCheckoutAsPage.single", remoteFolders[0].getName())); //$NON-NLS-1$		
		}
		
		wizardButton = new Button(outerContainer, SWT.RADIO);
		wizardButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.wizard")); //$NON-NLS-1$
		
		projectButton = new Button(outerContainer, SWT.RADIO);
		projectButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.project")); //$NON-NLS-1$

		Composite projectGroup = new Composite(outerContainer,SWT.NONE);
		GridLayout projectLayout = new GridLayout();
		projectLayout.numColumns = 2;
		projectGroup.setLayout(projectLayout);
		projectGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Label projectLabel = new Label(projectGroup, SWT.NONE);
		projectLabel.setText(Policy.bind("CheckoutWizardCheckoutAsPage.projectName")); //$NON-NLS-1$
		projectText = new Text(projectGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		projectText.setLayoutData(data);
		if (remoteFolders != null) projectText.setText(remoteFolders[0].getName());
		projectText.setEnabled(false);
		projectText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				CheckoutWizard wizard = (CheckoutWizard)getWizard();
				wizard.setProjectName(projectText.getText().trim());
				setPageComplete(canFinish());
			}			
		});
		
		existingButton = new Button(outerContainer, SWT.RADIO);
		existingButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.existing")); //$NON-NLS-1$
		existingButton.setEnabled(false);
		existingButton.setVisible(false);
		
		wizardButton.setSelection(true);
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				projectText.setEnabled(projectButton.getSelection());
				if (projectButton.getSelection()) {
					projectText.selectAll();
					projectText.setFocus();
				}
				setPageComplete(canFinish());
			}
		};
		
		wizardButton.addSelectionListener(selectionListener);
		projectButton.addSelectionListener(selectionListener);
		existingButton.addSelectionListener(selectionListener);
		
		setMessage(Policy.bind("CheckoutWizardCheckoutAsPage.text")); //$NON-NLS-1$
		
		setControl(outerContainer);	
	}
	
	public void setText(String text) {
		textLabel.setText(text);
	}
	
	public void setProject(String project) {
		projectText.setText(project);
	}
	
	public boolean useWizard() {
		return wizardButton.getSelection();
	}
	
	private boolean canFinish() {
		return !projectButton.getSelection() || projectText.getText().trim().length() > 0;
	}

}
