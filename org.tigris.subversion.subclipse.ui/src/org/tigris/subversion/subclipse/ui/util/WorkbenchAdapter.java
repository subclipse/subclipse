/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A default implementation of the IWorkbenchAdapter interface.
 * Provides basic implementations of the interface methods.
 */
public abstract class WorkbenchAdapter implements IWorkbenchAdapter {
	protected static final Object[] NO_CHILDREN = new Object[0];
/**
 * @see IWorkbenchAdapter#getChildren
 */
public Object[] getChildren(Object o) {
	return NO_CHILDREN;
}
/**
 * A convenience method for getting the label of an adaptable
 * object that responds to the IWorkbenchAdapter adapter interface.
 * Returns a toString() of the object if it is not adaptable
 */
public static String getElementLabel(IAdaptable a) {
	if (a == null) {
		return "";//$NON-NLS-1$
	}
	IWorkbenchAdapter adapter = (IWorkbenchAdapter)a.getAdapter(IWorkbenchAdapter.class);
	if (adapter != null) {
		return adapter.getLabel(a);
	} else {
		return a.toString();
	}
}
/**
 * @see IWorkbenchAdapter#getImageDescriptor
 */
public ImageDescriptor getImageDescriptor(Object object) {
	return null;
}
/**
 * @see IWorkbenchAdapter#getLabel
 */
public String getLabel(Object o) {
	return o == null ? "" : o.toString();//$NON-NLS-1$
}
/**
 * @see IWorkbenchAdapter#getParent
 */
public Object getParent(Object o) {
	return null;
}
}

