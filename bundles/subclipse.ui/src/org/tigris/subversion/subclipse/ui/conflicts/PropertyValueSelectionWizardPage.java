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
package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class PropertyValueSelectionWizardPage extends WizardPage {
	private SVNConflictDescriptor conflictDescriptor;
	private IResource resource;
	private String myValue;
	private String incomingValue;
	private IDialogSettings settings;
	private Button myValueButton;
	private Text myValueText;
	private Button incomingValueButton;
	private Text incomingValueText;
	private final static String LAST_CHOICE = "PropertyValueSelectionWizardPage.lastChoice"; //$NON-NLS-1$

	public PropertyValueSelectionWizardPage(String pageName) {
		super(pageName, Messages.PropertyValueSelectionWizardPage_0, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN));
		settings = SVNUIPlugin.getPlugin().getDialogSettings();	
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);	
		
		IResource resource = SVNWorkspaceRoot.getResourceFor(this.resource, new Path(conflictDescriptor.getPath()));		
		Label resourceLabel = new Label(composite, SWT.NONE);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		resourceLabel.setLayoutData(data);
		resourceLabel.setText(Messages.PropertyValueSelectionWizardPage_1);
		Label resourceNameLabel = new Label(composite, SWT.WRAP);
		resourceNameLabel.setText(resource.getFullPath().makeRelative().toOSString());
		data = new GridData();
		data.widthHint = 500;
		resourceNameLabel.setLayoutData(data);
		
		Label propertyLabel = new Label(composite, SWT.NONE);
		propertyLabel.setText(Messages.PropertyValueSelectionWizardPage_2);
		Label propertyNameLabel  = new Label(composite, SWT.WRAP);
		propertyNameLabel.setText(conflictDescriptor.getPropertyName());
		data = new GridData();
		data.widthHint = 500;
		propertyNameLabel.setLayoutData(data);
		
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);	
		
		myValueButton = new Button(composite, SWT.RADIO);
		data = new GridData();
		data.horizontalSpan = 2;
		myValueButton.setLayoutData(data);
		myValueButton.setText(Messages.PropertyValueSelectionWizardPage_3);
		
		myValueText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		data.heightHint = 100;
		data.widthHint = 400;
		data.horizontalIndent = 30;
		data.grabExcessHorizontalSpace = true;
		myValueText.setLayoutData(data);	
		myValueText.setText(myValue);
		
		incomingValueButton = new Button(composite, SWT.RADIO);
		data = new GridData();
		data.horizontalSpan = 2;
		incomingValueButton.setLayoutData(data);
		incomingValueButton.setText(Messages.PropertyValueSelectionWizardPage_4);
		
		incomingValueText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		data.heightHint = 100;
		data.widthHint = 400;
		data.horizontalIndent = 30;
		data.grabExcessHorizontalSpace = true;
		incomingValueText.setLayoutData(data);	
		incomingValueText.setText(incomingValue);		
		
		String lastChoice = settings.get(LAST_CHOICE);
		if (lastChoice != null && lastChoice.equals("incoming")) { //$NON-NLS-1$
			myValueText.setEditable(false);
			incomingValueButton.setSelection(true);
			incomingValueText.setFocus();
		} else {
			incomingValueText.setEditable(false);
			myValueButton.setSelection(true);
			myValueText.setFocus();
		}
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (incomingValueButton.getSelection()) {
					settings.put(LAST_CHOICE, "incoming"); //$NON-NLS-1$
					incomingValueText.setEditable(true);
					myValueText.setEditable(false);
					incomingValueText.setFocus();
				} else {
					settings.put(LAST_CHOICE, "mine"); //$NON-NLS-1$
					myValueText.setEditable(true);
					incomingValueText.setEditable(false);
					myValueText.setFocus();
				}
			}	
		};
		
		myValueButton.addSelectionListener(selectionListener);
		incomingValueButton.addSelectionListener(selectionListener);
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		myValueText.addFocusListener(focusListener);
		incomingValueText.addFocusListener(focusListener);
		
		setMessage(Messages.PropertyValueSelectionWizardPage_5);
		
		setControl(outerContainer);
	}
	
	public String getValue() {
		if (myValueButton.getSelection()) return myValueText.getText().trim();
		else return incomingValueText.getText().trim();
	}

	public void setConflictDescriptor(SVNConflictDescriptor conflictDescriptor) {
		this.conflictDescriptor = conflictDescriptor;
	}

	public void setMyValue(String myValue) {
		this.myValue = myValue;
	}

	public void setIncomingValue(String incomingValue) {
		this.incomingValue = incomingValue;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}

}
