/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.sharing;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

/**
 * wizard page to select remote directory that will correpond to the project 
 */
public class DirectorySelectionPage extends SVNWizardPage {
	Button useProjectNameButton;
	Button useSpecifiedNameButton;
	Text text;
	Button browseButton;
	Text urlText;
	
	String result;
	boolean useProjectName = true;
	
	public DirectorySelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 3);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_MODULE_PAGE);
		
		useProjectNameButton = createRadioButton(composite, Policy.bind("ModuleSelectionPage.moduleIsProject"), 3); //$NON-NLS-1$
		useSpecifiedNameButton = createRadioButton(composite, Policy.bind("ModuleSelectionPage.specifyModule"), 1); //$NON-NLS-1$
		useProjectNameButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
			    setUrlText();
				useProjectName = useProjectNameButton.getSelection();
				if (useProjectName) {
					text.setEnabled(false);
					browseButton.setEnabled(false);
					result = null;
					setPageComplete(true);
				} else {
					text.setEnabled(true);
					browseButton.setEnabled(true);
					result = text.getText();
					if (result.length() == 0) {
						result = null;
						setPageComplete(false);
					} else {
						setPageComplete(true);
					}
				}
			}
		});

		text = createTextField(composite);
		text.setEnabled(false);
		text.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
			    setUrlText();
				result = text.getText().trim();
				if (result.length() == 0) {
					result = null;
					setPageComplete(false);
				} else {
					setPageComplete(true);
				}
			}
		});
		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(Policy.bind("SharingWizard.browse")); //$NON-NLS-1$
		browseButton.setEnabled(false);
		browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent se) {
                SharingWizard wizard = (SharingWizard)getWizard();
                try {
                   ISVNRepositoryLocation repositoryLocation = wizard.getLocation();
                   ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), wizard.getProject());
                   dialog.setRepositoryLocation(repositoryLocation);
                   if (dialog.open() == ChooseUrlDialog.OK && dialog.getUrl() != null) {
                       text.setText(dialog.getUrl().toString().substring(repositoryLocation.getLocation().length() + 1) + "/New Folder");
                       text.setFocus();
                       text.setSelection(text.getText().indexOf("/New Folder") + 1, text.getText().length());
                   }                    
                } catch (Exception e) {}
            }
		});
		Group urlGroup = new Group(composite, SWT.NONE);
		urlGroup.setText(Policy.bind("SharingWizard.url")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		urlGroup.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		urlGroup.setLayoutData(data);
		urlText = createTextField(urlGroup);
		urlText.setEditable(false);
		
		Label warningLabel = new Label(composite, SWT.NONE);
		warningLabel.setText(Policy.bind("SharingWizard.cannotExist")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		warningLabel.setLayoutData(data);
		
		useSpecifiedNameButton.setSelection(false);
		useProjectNameButton.setSelection(true);
		setUrlText();
		setControl(composite);
		setPageComplete(true);
	}
    
    /**
     * null if "use Project name"  
     */	
	public String getDirectoryName() {
		return result;
	}
    
	public boolean useProjectName() {
		return useProjectName;
	}
    
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			useProjectNameButton.setFocus();
			setUrlText();
		}
	}
	
	private void setUrlText() {
		SharingWizard wizard = (SharingWizard)getWizard();
		try {
		    if (useProjectNameButton.getSelection()) urlText.setText(wizard.getLocation().getLocation() + "/" + wizard.getProject().getName());
		    else urlText.setText(wizard.getLocation().getLocation() + "/" + text.getText().trim());
		} catch (Exception e) {}	    
	}
}
