/*******************************************************************************
 * Copyright (c) 2010 Subclipse project and others.
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.tools.usage.util.collectionfilter;

public class CompositeCollectionFilter implements ICollectionFilter {

	private ICollectionFilter filters[];

	/**
	 * Instantiates a new composite filter that applies several given
	 * filters.
	 * 
	 * @param filters
	 *            the filters
	 */
	public CompositeCollectionFilter(ICollectionFilter[] filters) {
		this.filters = filters;
	}

	/**
	 * Applies the filters this composite filter has. All filters have to
	 * match so that the filter says the given bundle matches.
	 */
	public boolean matches(Object object) {
		for (int i = 0; i < filters.length; i++) {
			if (!filters[i].matches(object))	 {
				return false;
			}
		}
		return true;
	}
}