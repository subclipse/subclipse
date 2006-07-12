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
package org.tigris.subversion.subclipse.ui.wizards.generatediff;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;


/**
 * Page to select the options for creating the patch.
 */
public class PatchFileCreationOptionsPage extends WizardPage {
	

	private Button recurseOption;
	
	/**
	 * Constructor for PatchFileCreationOptionsPage.
	 */
	protected PatchFileCreationOptionsPage(GenerateDiffFileWizard wizard, String pageName) {
		super(pageName);
		
	}

	/**
	 * Constructor for PatchFileCreationOptionsPage.
	 */
	protected PatchFileCreationOptionsPage(GenerateDiffFileWizard wizard, String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData());
		setControl(composite);

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PATCH_OPTIONS_PAGE);
					
		recurseOption = new Button(composite, SWT.CHECK);
		recurseOption.setText(Policy.bind("GenerateSVNDiff.RecurseOption")); //$NON-NLS-1$
		recurseOption.setSelection(true);
	}
	
	/**
	 * Answers if the difference operation should be run recursively.
	 */
	public boolean isRecursive() {
		return recurseOption.getSelection();
	}
	
   
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			recurseOption.setFocus();
		}
	}
}