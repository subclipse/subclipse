/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Display a message with a details that can contain a list of projects
 */
public class DetailsDialogWithProjects extends DetailsDialog {
	
	private String message;
	private String detailsTitle;
	private IProject[] projects;

	
	private org.eclipse.swt.widgets.List detailsList;

	private boolean includeCancelButton;

	/**
	 * Constructor for DetailsDialogWithProjects.
	 * @param parentShell
	 * @param dialogTitle
	 */
	public DetailsDialogWithProjects(Shell parentShell, String dialogTitle, String dialogMessage, String detailsTitle, IProject[] projects, boolean includeCancelButton, Image image) {
		super(parentShell, dialogTitle);
		setImage(image);
		this.message = dialogMessage;
		this.detailsTitle = detailsTitle;
		this.projects = projects;
		this.includeCancelButton = includeCancelButton;
	}

	/**
	 * @see DetailsDialog#createMainDialogArea(Composite)
	 */
	protected void createMainDialogArea(Composite composite) {
		Label label = new Label(composite, SWT.WRAP);
		label.setText(message); //$NON-NLS-1$
		GridData data = new GridData(
			GridData.GRAB_HORIZONTAL |
			GridData.GRAB_VERTICAL |
			GridData.HORIZONTAL_ALIGN_FILL |
			GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		label.setFont(composite.getFont());
		updateEnablements();
	}

	/**
	 * @see DetailsDialog#createDropDownDialogArea(Composite)
	 */
	protected Composite createDropDownDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());
		
		detailsList = new org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);	 
		GridData data = new GridData ();		
		data.heightHint = 75;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		detailsList.setLayoutData(data);
		
		if (detailsTitle != null) {
			detailsList.add(detailsTitle);
		}
		
		for (int i = 0; i < projects.length; i++) {
			
			detailsList.add(projects[i].getName()); //$NON-NLS-1$
		}			
		return composite;
	}

	/**
	 * @see DetailsDialog#updateEnablements()
	 */
	protected void updateEnablements() {
		setPageComplete(true);
	}

	/**
	 * @see DetailsDialog#includeCancelButton()
	 */
	protected boolean includeCancelButton() {
		return includeCancelButton;
	}

}
