/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * The EmptySearchViewerFilter is a ViewerFilter that can be applied
 * to a table in order to filter all entries out of the table.
 */
public class EmptySearchViewerFilter extends ViewerFilter {

	/***
	 * Construct a new EmptySearchViewerFilter
	 */
	public EmptySearchViewerFilter() {
	}

	/**
     * Returns always false in order to generate 
     * @param viewer the viewer
     * @param parentElement the parent element
     * @param element the element
     * @return <code>false</code>
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return false;
	}

}
