/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards;


import java.util.Properties;

import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Wizard to add a new remote folder
 */
public class NewRemoteFolderWizard extends Wizard {
	private NewRemoteFolderWizardMainPage mainPage;

	private Properties properties = null;
   
	public NewRemoteFolderWizard() {
		setWindowTitle(Policy.bind("NewLocationWizard.title")); //$NON-NLS-1$
	}
	
	/**
	 * Creates the wizard pages
	 */
	public void addPages() {
		mainPage = new NewRemoteFolderWizardMainPage("repositoryPage1", Policy.bind("NewLocationWizard.heading"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION)); //$NON-NLS-1$ //$NON-NLS-2$
		addPage(mainPage);
	}
	/*
	 * @see IWizard#performFinish
	 */
	public boolean performFinish() {
	   return false;
	}
}
