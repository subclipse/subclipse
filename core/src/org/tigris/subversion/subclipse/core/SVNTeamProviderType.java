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
package org.tigris.subversion.subclipse.core;

import org.eclipse.team.core.RepositoryProviderType;


/**
 * This class represents the SVN Provider's capabilities in the absence of a
 * particular project.
 */

public class SVNTeamProviderType extends RepositoryProviderType {
	
	/**
	 * @see org.eclipse.team.core.RepositoryProviderType#supportsProjectSetImportRelocation()
	 */
	public boolean supportsProjectSetImportRelocation() {
		return false;
	}


}
