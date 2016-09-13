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
package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Extend this class to provide a wizard page from which an SVN repository
 * can be selected.
 */
public abstract class SVNRepositoryProviderWizardPage extends WizardPage {

	public SVNRepositoryProviderWizardPage(String pageName, String title) {
		super(pageName, title, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION));
	}

	/**
	 * 
	 * @return the URL of the selected SVN repository
	 */
	public abstract String getSelectedUrl();

}
