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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.tigris.subversion.subclipse.ui.Policy;

public class CheckoutWizardProjectPage extends WizardPage {
	private Button defaultButton;
	private Label locationLabel;
	private Text locationText;
	private Button browseButton;
	private Button addToWorkingSetsButton;
	private Label workingSetsLabel;
	private Combo workingSetsCombo;
	private Button workingSetsSelectButton;
	private IWorkingSet[] workingSets;
	private Map<String, IWorkingSet[]> workingSetsMap = new HashMap<String, IWorkingSet[]>();

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
		
		Composite workingSetsComposite = new Composite(outerContainer,SWT.NONE);
		GridLayout workingSetsLayout = new GridLayout();
		workingSetsLayout.numColumns = 3;
		workingSetsLayout.marginHeight = 0;
		workingSetsLayout.marginWidth = 0;
		workingSetsComposite.setLayout(workingSetsLayout);
		data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 3;
		workingSetsComposite.setLayoutData(data);
		
		addToWorkingSetsButton = new Button(workingSetsComposite, SWT.CHECK);
		addToWorkingSetsButton.setText(Policy.bind("CheckoutWizardProjectPage.0")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		addToWorkingSetsButton.setLayoutData(data);
		addToWorkingSetsButton.addSelectionListener(new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				setWorkingSetsEnablement();
			}
		});
		
		workingSetsLabel = new Label(workingSetsComposite, SWT.NONE);
		workingSetsLabel.setText(Policy.bind("CheckoutWizardProjectPage.1")); //$NON-NLS-1$
		workingSetsCombo = new Combo(workingSetsComposite, SWT.BORDER | SWT.READ_ONLY);
		workingSetsCombo.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		workingSetsCombo.addSelectionListener(new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				workingSets = workingSetsMap.get(workingSetsCombo.getText());
			}
		});
		
		workingSetsSelectButton = new Button(workingSetsComposite, SWT.PUSH);
		workingSetsSelectButton.setText(Policy.bind("CheckoutWizardProjectPage.2")); //$NON-NLS-1$
		workingSetsSelectButton.addSelectionListener(new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
				IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(getShell(), true);
				if (workingSets != null) {
					dialog.setSelection(workingSets);
				}
				dialog.open();
				workingSets = dialog.getSelection();
				if (workingSets != null) {
					String workingSetsString = getWorkingSetsString(workingSets);
					if (workingSetsMap.get(workingSetsString) == null) {
						workingSetsMap.put(workingSetsString, workingSets);
						workingSetsCombo.add(workingSetsString);
					}
					workingSetsCombo.setText(workingSetsString);
				}
				else {
					addToWorkingSetsButton.setSelection(false);
					setWorkingSetsEnablement();
				}
			}
		});
		
		setWorkingSetsEnablement();
		
		// Don't show working set section if Eclipse API doesn't support it.
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		Class[] parameterTypes = { IAdaptable.class, IWorkingSet[].class };
		Method addToWorkingSets = null;
		try {
			addToWorkingSets = manager.getClass().getMethod("addToWorkingSets", parameterTypes);				
		} catch (Exception e) {}
		if (addToWorkingSets == null) {
			addToWorkingSetsButton.setVisible(false);
			workingSetsLabel.setVisible(false);
			workingSetsCombo.setVisible(false);
			workingSetsSelectButton.setVisible(false);
		}
		
		setMessage(Policy.bind("CheckoutWizardProjectPage.text")); //$NON-NLS-1$
		
		setControl(outerContainer);	
	}
	
	public IWorkingSet[] getWorkingSets() {
		if (addToWorkingSetsButton.getSelection()) {
			return workingSets;
		}
		else {
			return null;
		}
	}
	
	private String getWorkingSetsString(IWorkingSet[] workingSets) {
		if (workingSets == null) {
			return null;
		}
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < workingSets.length; i++) {
			if (i > 0) {
				buffer.append(", "); //$NON-NLS-1$
			}
			buffer.append(workingSets[i].getName());
		}
		return buffer.toString();
	}
	
	private void setWorkingSetsEnablement() {
		workingSetsLabel.setEnabled(addToWorkingSetsButton.getSelection());
		workingSetsCombo.setEnabled(addToWorkingSetsButton.getSelection());
		workingSetsSelectButton.setEnabled(addToWorkingSetsButton.getSelection());
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
