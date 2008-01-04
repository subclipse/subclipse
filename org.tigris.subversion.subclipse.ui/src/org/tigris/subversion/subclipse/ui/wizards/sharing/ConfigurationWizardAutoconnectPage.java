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


import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.repo.SVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

/**
 * This configuration page explains to the user that .svn/ directories already exists and
 * it will attach the selected project to the repository that is specified in the .svn/ files.
 * 
 * This is useful for people who have checked out a project using command-line tools.
 */
public class ConfigurationWizardAutoconnectPage extends SVNWizardPage {
	private boolean validate = true;
	private LocalResourceStatus status;  // the status of the project directory
	ISVNRepositoryLocation location;     // the repository location of the project directory

	public ConfigurationWizardAutoconnectPage(String pageName, String title, ImageDescriptor titleImage, LocalResourceStatus status) {
		super(pageName, title, titleImage);
		this.status = status;
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 2);
		setControl(composite);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_AUTOCONNECT_PAGE);
		
		Label description = createWrappingLabel(composite, Policy.bind("RepositorySelectionPage.description"), 0 /* indent */, 2 /* columns */); //$NON-NLS-1$
		description.setText(Policy.bind("ConfigurationWizardAutoconnectPage.description")); //$NON-NLS-1$
		
		if (location == null) return;

		// Spacer
		createLabel(composite, ""); //$NON-NLS-1$
		createLabel(composite, ""); //$NON-NLS-1$
		
		createLabel(composite, Policy.bind("ConfigurationWizardAutoconnectPage.user")); //$NON-NLS-1$
		createLabel(composite, location.getUsername());
		createLabel(composite, Policy.bind("ConfigurationWizardAutoconnectPage.host")); //$NON-NLS-1$
		createLabel(composite, location.getUrl().toString());
        
		// Spacer
		createLabel(composite, ""); //$NON-NLS-1$
		createLabel(composite, ""); //$NON-NLS-1$
		
		final Button check = new Button(composite, SWT.CHECK);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		check.setLayoutData(data);
		check.setText(Policy.bind("ConfigurationWizardAutoconnectPage.validate")); //$NON-NLS-1$
		check.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				validate = check.getSelection();
			}
		});
		check.setSelection(true);		
	}
	
	public boolean getValidate() {
		return validate;
	}
    
    /**
     * set the project that will be shared 
     */
	public void setProject(IProject project) {
		try {
			if (status == null) {
				// This should never happen
				SVNUIPlugin.openError(null, Policy.bind("ConfigurationWizardAutoconnectPage.noSyncInfo"), Policy.bind("ConfigurationWizardAutoconnectPage.noSVNDirectory"), null); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			location = SVNRepositoryLocation.fromString(status.getUrlString());
		} catch (TeamException e) {
			SVNUIPlugin.openError(null, null, null, e);
		}
	}
	
	/**
     * gets the status of the directory corresponding to the project 
	 */
	public LocalResourceStatus getSharingStatus() {
		return status;
	}
	/**
	 * Gets the repository location of the project
	 * @return Returns a ISVNRepositoryLocation
	 */
	public ISVNRepositoryLocation getLocation() {
		return location;
	}

}
