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
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.tigris.subversion.subclipse.core.resources.BaseResource;
import org.tigris.subversion.subclipse.core.resources.BaseResourceStorage;

/**
 * This class does not add any functionality. It just adds the UI dependent interface
 * <code>IEncodedStreamContentAccessor</code>.
 * @see org.tigris.subversion.subclipse.core.resources.BaseResourceStorageFactory#current()
 */
public class UIBaseResourceStorage extends BaseResourceStorage implements IEncodedStreamContentAccessor {

	/** Do not use this constructor directly.
	 * @param baseResource
	 */
	protected UIBaseResourceStorage(BaseResource baseResource) {
		super(baseResource);
	}
}
