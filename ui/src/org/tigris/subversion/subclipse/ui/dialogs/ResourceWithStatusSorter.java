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
package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ResourceWithStatusSorter extends ViewerSorter {
	private boolean reversed = false;
	private int sortedColumnNumber;
	private static final int NUM_COLUMNS = 4;
	private static final int[][] SORT_ORDERS_BY_COLUMN = {
	    {0, 1, 2, 3}, 	/* check */    
		{1, 0, 2, 3},	/* resource */ 
		{2, 0, 1, 3},	/* status */
		{3, 0, 1, 2},	/* prop status */
	};
	
	public ResourceWithStatusSorter(int sortedColumnNumber) {
		this.sortedColumnNumber = sortedColumnNumber;
	}
	
	public int compare(Viewer viewer, Object e1, Object e2) {
		IResource r1 = (IResource)e1;
		IResource r2 = (IResource)e2;
		int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[sortedColumnNumber];
		int result = 0;
		for (int i = 0; i < NUM_COLUMNS; ++i) {
			result = compareColumnValue(columnSortOrder[i], r1, r2);
			if (result != 0)
				break;
		}
		if (reversed)
			result = -result;
		return result;
	}
	
	private int compareColumnValue(int columnNumber, IResource r1, IResource r2) {
		switch (columnNumber) {
			case 0: /* check */
				return 0;
			case 1: /* resource */
				return collator.compare(r1.getFullPath().toString(), r2.getFullPath().toString());					
			case 2: /* status */
				return collator.compare(ResourceWithStatusUtil.getStatus(r1), ResourceWithStatusUtil.getStatus(r2));
			case 3: /* prop status */
				return collator.compare(ResourceWithStatusUtil.getPropertyStatus(r1), ResourceWithStatusUtil.getPropertyStatus(r2));					
			default:
				return 0;
		}
	}

	public int getColumnNumber() {
		return sortedColumnNumber;
	}

	public boolean isReversed() {
		return reversed;
	}

	public void setReversed(boolean newReversed) {
		reversed = newReversed;
	}
}
