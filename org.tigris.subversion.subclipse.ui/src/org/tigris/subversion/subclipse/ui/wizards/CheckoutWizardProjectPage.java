/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.ui.Policy;

public class CheckoutWizardProjectPage extends WizardPage {
	private Button defaultButton;
	private Label locationLabel;
	private Text locationText;
	private Button browseButton;

	public CheckoutWizardProjectPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		defaultButton = new Button(outerContainer, SWT.CHECK);
		GridData data = new GridData();
		data.horizontalSpan = 3;
		defaultButton.setLayoutData(data);
		defaultButton.setText(Policy.bind("CheckoutWizardProjectPage.default")); //$NON-NLS-1$
		defaultButton.setSelection(true);
		defaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setLocationEnablement();
				if (!defaultButton.getSelection()) {
					locationText.selectAll();
					locationText.setFocus();
				}
				setPageComplete();
			}
		});
		
		locationLabel = new Label(outerContainer, SWT.NONE);
		locationLabel.setText(Policy.bind("CheckoutWizardProjectPage.location")); //$NON-NLS-1$
		locationText = new Text(outerContainer, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		locationText.setLayoutData(data);
		setLocation();
		locationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete();			
			}			
		});
		
		browseButton = new Button(outerContainer, SWT.PUSH);
		browseButton.setText(Policy.bind("CheckoutWizardProjectPage.browse")); //$NON-NLS-1$
		
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage(Policy.bind("CheckoutInto.message")); //$NON-NLS-1$
				String directory = dialog.open();
				if (directory != null) {
					locationText.setText(directory);
				}
			}
		});
		
		setLocationEnablement();
		
		setMessage(Policy.bind("CheckoutWizardProjectPage.text")); //$NON-NLS-1$
		
		setControl(outerContainer);	
	}

	private void setLocationEnablement() {
		locationLabel.setEnabled(!defaultButton.getSelection());
		locationText.setEnabled(!defaultButton.getSelection());
		browseButton.setEnabled(!defaultButton.getSelection());
	}
	
	private void setPageComplete() {
		setPageComplete(defaultButton.getSelection() || locationText.getText().trim().length() > 0);
	}
	
	public void setLocation() {
		locationText.setText(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
	}
	
	public String getLocation() {
		if (locationText == null) {
			CheckoutWizard wizard = (CheckoutWizard)getWizard();
			return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + File.separator + wizard.getProjectName();
		} else return locationText.getText().trim();
	}
	
	public String getCanonicalLocation() {
		return normalizeCase(getLocation());
	}
	
	private String normalizeCase(String location) {
		File dir = new File(location);
		String caseFixed;
		String original= dir.getAbsolutePath();
		try {
			caseFixed = dir.getCanonicalPath();
		} catch (IOException e) {
			return location;
		}
		// Make sure the path name did not change.  If the
		// path is a symlink, then getCanonical will change
		// the path to the real path and we just have to go
		// with the original.
		if (caseFixed.equalsIgnoreCase(original))
			return caseFixed;
		else
			return location;
	}

}
