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
