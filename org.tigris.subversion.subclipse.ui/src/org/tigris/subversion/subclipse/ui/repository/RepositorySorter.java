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
package org.tigris.subversion.subclipse.ui.repository;

import org.eclipse.jface.viewers.ViewerSorter;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

public class RepositorySorter extends ViewerSorter {

	private static final int REPO_ROOT_CATEGORY = 1;
	private static final int REMOTE_FOLDER_CATEGORY = 2;
	private static final int REMOTE_FILE_CATEGORY = 3;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
	 */
	public int category(Object element) {
		if (element instanceof ISVNRepositoryLocation) {
			return REPO_ROOT_CATEGORY;
		}
		
		if (element instanceof ISVNRemoteFolder) {
			return REMOTE_FOLDER_CATEGORY;
		}
		
		if (element instanceof ISVNRemoteFile) {
			return REMOTE_FILE_CATEGORY;
		}
		
		return 0;
	}
}
