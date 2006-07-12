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
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ResourceLabelProvider extends WorkbenchLabelProvider implements
        ITableLabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
	    if (columnIndex == 0) {
			return getImage(element);
	    }
		return null;
    }

    public String getColumnText(Object element, int columnIndex) {
 	   String result = null;
	   switch (columnIndex) {
		   case 0 :
			   result = ((IResource)element).getFullPath().toString();
			   break;	
		   default:
			   result = ""; //$NON-NLS-1$
		   break;
	   }
	   
	   return result;
    }

}
