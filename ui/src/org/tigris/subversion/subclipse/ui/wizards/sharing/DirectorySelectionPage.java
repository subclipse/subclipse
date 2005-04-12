/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.sharing;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

/**
 * wizard page to select remote directory that will correpond to the project 
 */
public class DirectorySelectionPage extends SVNWizardPage {
	Button useProjectNameButton;
	Button useSpecifiedNameButton;
	Text text;
	
	String result;
	boolean useProjectName = true;
	
	public DirectorySelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 2);
		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.SHARING_MODULE_PAGE);
		
		useProjectNameButton = createRadioButton(composite, Policy.bind("ModuleSelectionPage.moduleIsProject"), 2); //$NON-NLS-1$
		useSpecifiedNameButton = createRadioButton(composite, Policy.bind("ModuleSelectionPage.specifyModule"), 1); //$NON-NLS-1$
		useProjectNameButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				useProjectName = useProjectNameButton.getSelection();
				if (useProjectName) {
					text.setEnabled(false);
					result = null;
					setPageComplete(true);
				} else {
					text.setEnabled(true);
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
				result = text.getText();
				if (result.length() == 0) {
					result = null;
					setPageComplete(false);
				} else {
					setPageComplete(true);
				}
			}
		});
		useSpecifiedNameButton.setSelection(false);
		useProjectNameButton.setSelection(true);
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
		}
	}
}
