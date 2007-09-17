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
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ResourceWithStatusLabelProvider extends WorkbenchLabelProvider implements ITableLabelProvider {


	private final String baseUrl;

	public ResourceWithStatusLabelProvider(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	public String getColumnText(Object element, int columnIndex) {
	   String result = null;
	   switch (columnIndex) {
		   case 0 :
			   result = "";  //$NON-NLS-1$
			   break;	
		   case 1:
			   if (this.baseUrl == null) result = ((IResource)element).getFullPath().toString();
			   else result = getResource((IResource)element);
			   if (result.length() == 0) result = ((IResource)element).getFullPath().toString();
			   break;
		   case 2:
			   result = ResourceWithStatusUtil.getStatus((IResource)element);
			   break;
		   case 3:
			   result = ResourceWithStatusUtil.getPropertyStatus((IResource)element);
			   break;	                
		   default:
			   result = ""; //$NON-NLS-1$
		   break;
	   }
	   
	   return result;
	}
	// Strip off segments of path that are included in URL.
	private String getResource(IResource resource) {
	    String[] segments = resource.getFullPath().segments();
	    StringBuffer path = new StringBuffer();
	    for (int i = 0; i < segments.length; i++) {
	        path.append("/" + segments[i]); //$NON-NLS-1$
	        if (this.baseUrl.endsWith(path.toString())) {
	            if (i == (segments.length - 2)) 
	                return resource.getFullPath().toString().substring(path.length() + 1);
	            else 
	                return resource.getFullPath().toString().substring(path.length());
	        }
	    }
	    return resource.getFullPath().toString();
    }
    public Image getColumnImage(Object element, int columnIndex) {
	    if (columnIndex == 1) {
			return getImage(element);
	    }
		return null;
	}
    public void addListener(ILabelProviderListener listener) {
    }
    public void dispose() {
    }
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }
    public void removeListener(ILabelProviderListener listener) {
    }
}