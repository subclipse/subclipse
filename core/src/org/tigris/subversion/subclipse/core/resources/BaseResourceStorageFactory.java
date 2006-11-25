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
package org.tigris.subversion.subclipse.core.resources;

/**
 * This is the default implementation of the factory and also the maintainer of
 * the <code>current factory</code>. When using the subclipse.core plugin is a headless mode, the
 * defualt implementation will suffice. The subclipse.ui plugin is expected to install
 * a new factory that in turn creates instances implementing the
 * <code>IEncodedStreamContentAccessor</code> (defined in org.eclipse.compare)
 * 
 * @author Thomas Hallgren
 */
public class BaseResourceStorageFactory implements IBaseResourceStorageFactory {

	private static IBaseResourceStorageFactory currentFactory = new BaseResourceStorageFactory();

	/**
	 * Creates an instance and returns it.
	 */
	public BaseResourceStorage createResourceStorage(BaseResource baseResource) {
		return new BaseResourceStorage(baseResource);
	}

	/**
	 * Returns the current factory.
	 */
	public static IBaseResourceStorageFactory current() {
		return currentFactory;
	}

	/**
	 * This method is called from the org.tigris.subversion.subclipse.ui plugin and will
	 * install a factory that creates {@link BaseResourceStorage} that implements the
	 *
	 * @param factory The factory to become current
	 */
	public static void setCurrent(IBaseResourceStorageFactory factory) {
		currentFactory = factory;
	}
}
