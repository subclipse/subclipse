package org.tigris.subversion.subclipse.ui.conflicts;

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
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class EditPropertyConflictsWizardPropertyPage extends WizardPage  {
	private PropertyConflict propertyConflict;
	private ISVNProperty remoteProperty;
	private String propertyValue;
	
	private Button myValueButton;
	private Text myValueText;
	private Button incomingValueButton;
	private Text incomingValueText;

	public EditPropertyConflictsWizardPropertyPage(PropertyConflict propertyConflict) {
		super(propertyConflict.getPropertyName(), propertyConflict.getPropertyName(), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN));
		this.propertyConflict = propertyConflict;
	}
	
	public void createControl(Composite parent) {
		EditPropertyConflictsWizard wizard = (EditPropertyConflictsWizard)getWizard();
		ISVNLocalResource svnResource = wizard.getSvnResource();
		try {
			ISVNProperty[] properties = svnResource.getSvnProperties();
			for (int i = 0; i < properties.length; i++) {
				if (properties[i].getName().equals(propertyConflict.getPropertyName())) {
					propertyValue = properties[i].getValue();
					break;
				}
			}
		} catch (SVNException e) {}
		
		ISVNProperty[] remoteProperties = wizard.getRemoteProperties();
		for (int i = 0; i < remoteProperties.length; i++) {
			if (remoteProperties[i].getName().equals(propertyConflict.getPropertyName())) {
				remoteProperty = remoteProperties[i];
				break;
			}
		}
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		outerContainer.setLayout(new GridLayout());
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		myValueButton = new Button(outerContainer, SWT.RADIO);
		myValueButton.setText(Messages.EditPropertyConflictsWizardPropertyPage_0);
		
		myValueText = new Text(outerContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 100;
		data.widthHint = 400;
		data.horizontalIndent = 30;
		data.grabExcessHorizontalSpace = true;
		myValueText.setLayoutData(data);	
		if (propertyValue != null) {
			myValueText.setText(propertyValue);
		}
		
		incomingValueButton = new Button(outerContainer, SWT.RADIO);
		incomingValueButton.setText(Messages.EditPropertyConflictsWizardPropertyPage_1);
		
		incomingValueText = new Text(outerContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		data.heightHint = 100;
		data.widthHint = 400;
		data.horizontalIndent = 30;
		data.grabExcessHorizontalSpace = true;
		incomingValueText.setLayoutData(data);	
		if (remoteProperty != null) {
			incomingValueText.setText(remoteProperty.getValue());	
		}
		
		myValueButton.setSelection(true);
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				if (incomingValueButton.getSelection()) {
					propertyValue = incomingValueText.getText();
				} else {
					propertyValue = myValueText.getText();
				}
			}		
		};
		
		myValueButton.addSelectionListener(selectionListener);
		incomingValueButton.addSelectionListener(selectionListener);
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				if (evt.getSource() == myValueText && myValueButton.getSelection()) {
					propertyValue = myValueText.getText();
				}
				if (evt.getSource() == incomingValueText && incomingValueButton.getSelection()) {
					propertyValue = incomingValueText.getText();
				}
			}			
		};
		
		myValueText.addModifyListener(modifyListener);
		incomingValueText.addModifyListener(modifyListener);
		
		setControl(outerContainer);
		
		setMessage(Messages.EditPropertyConflictsWizardPropertyPage_2 + propertyConflict.getPropertyName() + Messages.EditPropertyConflictsWizardPropertyPage_3);
	}
	
	public String getPropertyName() {
		return propertyConflict.getPropertyName();
	}
	
	public String getPropertyValue() {
		return propertyValue;
	}
	
	public boolean incomingSelected() {
		return incomingValueButton.getSelection();
	}
	
	public ISVNProperty getRemoteProperty() {
		return remoteProperty;
	}

}
