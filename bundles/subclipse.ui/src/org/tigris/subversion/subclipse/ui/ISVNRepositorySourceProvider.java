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
package org.tigris.subversion.subclipse.ui;

import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.ui.wizards.SVNRepositoryProviderWizardPage;

/**
 * This interface is implemented by contributors to the
 * org.tigris.subversion.subclipse.ui.svnRepositorySourceProviders
 * extension point.
 */
public interface ISVNRepositorySourceProvider {

	/**
	 * 
	 * @param id the id of the repository source provider
	 */
	public void setId(String id);

	/**
	 * 
	 * @return the id of the repository source provider
	 */
	public String getId();

	/**
	 * 
	 * @return the name that is to appear next to the repository source provider in the selection tree
	 */
	public String getName();

	/**
	 * 
	 * @return the image to appear next to the repository source provider in the selection tree
	 */
	public Image getImage();

	/**
	 * 
	 * @return the wizard page from which an SVN repository can be selected
	 */
	public SVNRepositoryProviderWizardPage getWizardPage();
}
