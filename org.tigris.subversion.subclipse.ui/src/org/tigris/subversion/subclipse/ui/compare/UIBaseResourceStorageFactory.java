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
package org.tigris.subversion.subclipse.ui.compare;
import org.tigris.subversion.subclipse.core.resources.BaseResource;
import org.tigris.subversion.subclipse.core.resources.BaseResourceStorage;
import org.tigris.subversion.subclipse.core.resources.IBaseResourceStorageFactory;

/**
 * This factory creates instances that has UI specific dependencies
 */
public class UIBaseResourceStorageFactory implements IBaseResourceStorageFactory {

	/**
	 * Creates a {@link org.tigris.subversion.subclipse.core.resources.BaseResourceStorage BaseResourceStorage} implementation
	 * that implements the {@link org.eclipse.compare.IEncodedStreamContentAccessor IEncodedStreamContentAccessor} interface.
	 * @return The created instance.
	 */
	public BaseResourceStorage createResourceStorage(BaseResource baseResource) {
		return new UIBaseResourceStorage(baseResource);
	}
}
