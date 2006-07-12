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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A list of adaptable objects.  This is a generic list that can
 * be used to display an arbitrary set of adaptable objects in the workbench.
 * Also implements the IWorkbenchAdapter interface for simple display
 * and navigation.
 */
public class AdaptableList extends WorkbenchAdapter implements IAdaptable {
	protected List children = null;
/**
 * Creates a new adaptable list.
 */
public AdaptableList() {
	children = new ArrayList();
}
/**
 * Creates a new adaptable list with the given size.
 */
public AdaptableList(int size) {
	children = new ArrayList(size);
}
/**
 * Creates a new adaptable list with the given children.
 */
public AdaptableList(IAdaptable[] newChildren) {
	this(newChildren.length);
	for (int i = 0; i < newChildren.length; i++) {
		children.add(newChildren[i]);
	}
}
/**
 * Adds all the adaptable objects in the given enumeration to this list.
 * Returns this list.
 */
public AdaptableList add(Iterator e) {
	while (e.hasNext()) {
		add((IAdaptable)e.next());
	}
	return this;
}
/**
 * Adds the given adaptable object to this list.  Returns this list.
 */
public AdaptableList add(IAdaptable a) {
	children.add(a);
	return this;
}
/**
 * Returns an object which is an instance of the given class
 * associated with this object. Returns <code>null</code> if
 * no such object can be found.
 */
public Object getAdapter(Class adapter) {
	if (adapter == IWorkbenchAdapter.class) return this;
	return null;
}
/**
 * Returns the elements in this list.
 */
public Object[] getChildren() {
	return children.toArray();
}
/**
 * Returns the elements in this list.
 * @see IWorkbenchAdapter#getChildren
 */
public Object[] getChildren(Object o) {
	return children.toArray();
}
/**
 * Adds the given adaptable object to this list.
 */
public void remove(IAdaptable a) {
	children.remove(a);
}
/**
 * Returns the number of items in the list
 */
public int size() {
	return children.size();
}
/**
 * For debugging purposes only.
 */
public String toString() {
	return children.toString();
}
}
