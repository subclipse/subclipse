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
package org.tigris.subversion.subclipse.ui.decorator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class ConflictResolutionGenerator implements IMarkerResolutionGenerator2 {
	private boolean textConflict;
	private boolean propertyConflict;
	private boolean treeConflict;
	
    public ConflictResolutionGenerator() {
        super();
    }

    public boolean hasResolutions(IMarker marker) {
        return true;
    }

    public IMarkerResolution[] getResolutions(IMarker marker) {
    	List conflictResolutions = new ArrayList();
    	try {
	    	if (marker.getAttribute("textConflict") != null && marker.getAttribute("textConflict").toString().equals("true")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		conflictResolutions.add(new EditConflictsResolution());
	    		conflictResolutions.add(new AcceptMineResolution());
	    		conflictResolutions.add(new AcceptTheirsResolution());
	    	}
    	} catch (Exception e) {
    		 SVNUIPlugin.log(e.getMessage());
    	}
    	conflictResolutions.add(new MarkAsResolvedResolution());
    	IMarkerResolution[] resolutionArray = new IMarkerResolution[conflictResolutions.size()];
    	conflictResolutions.toArray(resolutionArray);
        return resolutionArray;
    }
    
	public void setTextConflict(boolean textConflict) {
		this.textConflict = textConflict;
	}

	public void setPropertyConflict(boolean propertyConflict) {
		this.propertyConflict = propertyConflict;
	}

	public void setTreeConflict(boolean treeConflict) {
		this.treeConflict = treeConflict;
	}

}
