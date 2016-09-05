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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;

public class RelocateWizardUrlPage extends WizardPage {
	private String url;
	private Text newUrlText;

	public RelocateWizardUrlPage(String pageName, String title, ImageDescriptor titleImage, String url) {
		super(pageName, title, titleImage);
		this.url = url;
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(outerContainer, IHelpContextIds.RELOCATE_REPOSITORY_PAGE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Label urlLabel = new Label(outerContainer, SWT.NONE);
		urlLabel.setText(Policy.bind("RelocateWizard.urlLabel")); //$NON-NLS-1$
		Text urlText = new Text(outerContainer, SWT.BORDER);
		urlText.setEditable(false);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 600;
		urlText.setLayoutData(data);
		urlText.setText(url);
		
		Label newUrlLabel = new Label(outerContainer, SWT.NONE);
		newUrlLabel.setText(Policy.bind("RelocateWizard.newUrlLabel")); //$NON-NLS-1$
		newUrlText = new Text(outerContainer, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 600;
		newUrlText.setLayoutData(data);
		newUrlText.setText(url);
		
		newUrlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(newUrlText.getText().trim().length() > 0 && !newUrlText.getText().trim().equals(url));
			}			
		});

		setMessage(Policy.bind("RelocateWizard.newUrl")); //$NON-NLS-1$
		
		setControl(outerContainer);			
	}
	
	public String getNewUrl() {
		return newUrlText.getText().trim();
	}

}
