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


import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

/**
 * wizard page to select remote directory that will correpond to the project 
 */
public class DirectorySelectionPage extends SVNWizardPage {
	private ISVNRepositoryLocationProvider repositoryLocationProvider;
	Button useProjectNameButton;
	Button useSpecifiedNameButton;
	Text text;
	Button browseButton;
	Text urlText;
	
	String result;
	boolean useProjectName = true;
	private String lastLocation;
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	
	private static final String LAST_PARENT = "DirectorySelectionPage.lastParent_";
	
	public DirectorySelectionPage(String pageName, String title, ImageDescriptor titleImage, ISVNRepositoryLocationProvider repositoryLocationProvider) {
		super(pageName, title, titleImage);
		this.repositoryLocationProvider = repositoryLocationProvider;
	}
	
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 3);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_MODULE_PAGE);
		
		useProjectNameButton = createRadioButton(composite, Policy.bind("ModuleSelectionPage.moduleIsProject"), 3); //$NON-NLS-1$
		useSpecifiedNameButton = createRadioButton(composite, Policy.bind("ModuleSelectionPage.specifyModule"), 1); //$NON-NLS-1$

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
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		text.addFocusListener(focusListener);		
		
		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(Policy.bind("SharingWizard.browse")); //$NON-NLS-1$
		browseButton.setEnabled(false);
		browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent se) {
                try {
                   ISVNRepositoryLocation repositoryLocation = repositoryLocationProvider.getLocation();
                   ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), repositoryLocationProvider.getProject());
                   dialog.setRepositoryLocation(repositoryLocation);
                   if (dialog.open() == ChooseUrlDialog.OK && dialog.getUrl() != null) {
                       text.setText(dialog.getUrl().toString().substring(repositoryLocation.getLocation().length() + 1) + "/" + repositoryLocationProvider.getProject().getName());
                       text.setFocus();
                       text.setSelection(text.getText().indexOf(repositoryLocationProvider.getProject().getName()), text.getText().length());
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
		
		initializeSelection();
		
		setUrlText();
		
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
					text.setFocus();
					text.selectAll();
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
		
		setControl(composite);
		setPageComplete(true);
	}

	private void initializeSelection() {
		String lastParent = null;
		
		try {
			lastParent = settings.get(LAST_PARENT + repositoryLocationProvider.getLocation().getLocation());
			if (lastParent != null && lastParent.length() > 0) {
				useSpecifiedNameButton.setSelection(true);
				useProjectNameButton.setSelection(false);
				text.setEnabled(true);
				text.setFocus();
				browseButton.setEnabled(true);
				text.setText(lastParent + "/" + repositoryLocationProvider.getProject().getName());
				text.setSelection(text.getText().indexOf(repositoryLocationProvider.getProject().getName()), text.getText().length());
			}
		} catch (TeamException e1) {}
		
		if (lastParent == null) {
			useSpecifiedNameButton.setSelection(false);
			text.setText("");
			text.setEnabled(false);
			browseButton.setEnabled(false);
			useProjectNameButton.setSelection(true);			
		}
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
			if (useSpecifiedNameButton.getSelection()) {
				useSpecifiedNameButton.setFocus();
			}
			else {
				useProjectNameButton.setFocus();
			}
			try {
				String location = repositoryLocationProvider.getLocation().getLocation();
				if (lastLocation == null || !lastLocation.equals(location)) {
					initializeSelection();
				}
				lastLocation = location;
			} catch (TeamException e) {}
			setUrlText();
		}
	}
	
	private void setUrlText() {
		try {
		    if (useProjectNameButton.getSelection()) {
		    	urlText.setText(repositoryLocationProvider.getLocation().getLocation() + "/" + repositoryLocationProvider.getProject().getName());
		    	settings.put(LAST_PARENT + repositoryLocationProvider.getLocation().getLocation(), "");
		    }
		    else {
		    	urlText.setText(repositoryLocationProvider.getLocation().getLocation() + "/" + text.getText().trim());	
		    	int index = text.getText().lastIndexOf("/");
		    	if (index == -1) {
		    		settings.put(LAST_PARENT + repositoryLocationProvider.getLocation().getLocation(), "");
		    	}
		    	else {
		    		settings.put(LAST_PARENT + repositoryLocationProvider.getLocation().getLocation(), text.getText().substring(0, index));
		    	}
		    }
		} catch (Exception e) {}	    
	}
}
